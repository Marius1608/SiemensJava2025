package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    // Using a static executor service can cause issues with application shutdown
    // Better to use Spring's TaskExecutor for proper lifecycle management
    @Autowired
    @Qualifier("taskExecutor")
    private TaskExecutor taskExecutor;

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Processes all items asynchronously by updating their status to "PROCESSED".
     *
     * This implementation fixes several issues in the original code:
     * 1. Properly handles exceptions in individual futures without failing the entire batch
     * 2. Uses CompletableFuture composition for cleaner and more maintainable code
     * 3. Returns only successfully processed items
     * 4. Utilizes proper thread management through Spring's TaskExecutor
     * 5. Ensures all processing completes before returning the result
     * 6. Provides proper timeout handling
     *
     * @return List of successfully processed items
     * @throws RuntimeException if batch processing fails or times out
     */
    @Async
    public List<Item> processItemsAsync() {
        // Get all item IDs
        List<Long> itemIds = itemRepository.findAllIds();

        // Thread-safe collection for processed items
        List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());

        // Create a list of CompletableFutures for each item processing
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Process each item asynchronously
        for (Long id : itemIds) {
            CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                // Try to find and process the item, returning Optional.empty() on failure
                try {
                    // Simulate some processing time
                    Thread.sleep(100);

                    // Find item, update status, and save
                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isPresent()) {
                        Item item = optionalItem.get();
                        item.setStatus("PROCESSED");
                        Item savedItem = itemRepository.save(item);
                        processedItems.add(savedItem);
                        return Optional.of(savedItem);
                    }
                    return Optional.<Item>empty();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.<Item>empty();
                } catch (Exception e) {
                    // Log error but don't throw - allows other items to continue processing
                    System.err.println("Error processing item with ID " + id + ": " + e.getMessage());
                    return Optional.<Item>empty();
                }
            }, taskExecutor).thenAccept(optItem -> {
                // No additional action needed - we've already added the item to processedItems
                // in the supplyAsync block if processing was successful
            }).exceptionally(ex -> {
                // Handle any unexpected errors from the futures
                System.err.println("Unexpected error in future for item ID " + id + ": " + ex.getMessage());
                return null;
            });

            futures.add(future);
        }

        try {
            // Wait for all futures to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // Block until all items have been processed or timeout occurs

            allOf.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing was interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error during batch processing", e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Processing timed out after 30 seconds", e);
        }

        // Return the list of successfully processed items
        return new ArrayList<>(processedItems);
    }
}