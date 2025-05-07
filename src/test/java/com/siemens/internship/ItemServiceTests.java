package com.siemens.internship;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ItemServiceTests {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void testProcessItemsAsync() throws Exception {
        // Setup mock data
        Long id1 = 1L;
        Long id2 = 2L;

        Item item1 = new Item();
        item1.setId(id1);
        item1.setName("Item 1");

        Item item2 = new Item();
        item2.setId(id2);
        item2.setName("Item 2");

        // Mock repository behavior
        when(itemRepository.findAllIds()).thenReturn(Arrays.asList(id1, id2));
        when(itemRepository.findById(id1)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(id2)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        // Configure service to use synchronous executor for testing
        TaskExecutor syncExecutor = new SyncTaskExecutor();
        ReflectionTestUtils.setField(itemService, "taskExecutor", syncExecutor);

        // Execute the method
        List<Item> processedItems = itemService.processItemsAsync();

        // Verify results
        assertThat(processedItems).hasSize(2);
        assertThat(processedItems.get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(processedItems.get(1).getStatus()).isEqualTo("PROCESSED");

        // Verify interactions
        verify(itemRepository, times(1)).findAllIds();
        verify(itemRepository, times(1)).findById(id1);
        verify(itemRepository, times(1)).findById(id2);
        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void testProcessItemsAsync_WithSomeItemsNotFound() throws Exception {
        // Setup
        Long id1 = 1L;
        Long id2 = 2L;

        Item item1 = new Item();
        item1.setId(id1);
        item1.setName("Item 1");

        // Mock repository behavior - item2 not found
        when(itemRepository.findAllIds()).thenReturn(Arrays.asList(id1, id2));
        when(itemRepository.findById(id1)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(id2)).thenReturn(Optional.empty());
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        // Configure service to use synchronous executor for testing
        TaskExecutor syncExecutor = new SyncTaskExecutor();
        ReflectionTestUtils.setField(itemService, "taskExecutor", syncExecutor);

        // Execute
        List<Item> processedItems = itemService.processItemsAsync();

        // Verify
        assertThat(processedItems).hasSize(1);
        assertThat(processedItems.get(0).getId()).isEqualTo(id1);

        verify(itemRepository, times(1)).findAllIds();
        verify(itemRepository, times(1)).findById(id1);
        verify(itemRepository, times(1)).findById(id2);
        verify(itemRepository, times(1)).save(any(Item.class));
    }
}