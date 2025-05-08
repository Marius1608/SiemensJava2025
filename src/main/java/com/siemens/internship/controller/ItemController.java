package com.siemens.internship.controller;

import com.siemens.internship.service.ItemService;
import com.siemens.internship.model.Item;
import com.siemens.internship.exception.ResourceNotFoundException;
import com.siemens.internship.validator.EmailValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    //ResponseEntity with the list of items and HTTP status 200 (OK)
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    //ResponseEntity with the created item and HTTP status 201 (CREATED) or 400 (BAD_REQUEST) if validation fails
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            // Return validation errors
            List<String> errors = result.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        // Additional email validation
        if (item.getEmail() != null && !EmailValidator.isValid(item.getEmail())) {
            return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

    //ResponseEntity with the item and HTTP status 200 (OK) or 404 (NOT_FOUND) if the item doesn't exist
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    //ResponseEntity with the updated item and HTTP status 200 (OK) or 404 (NOT_FOUND) if the item doesn't exist
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        // Additional email validation
        if (item.getEmail() != null && !EmailValidator.isValid(item.getEmail())) {
            return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        return itemService.findById(id)
                .map(existingItem -> {
                    item.setId(id);
                    return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

    //ResponseEntity with HTTP status 204 (NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> {
                    itemService.deleteById(id);
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
    }

     //ResponseEntity with a list of processed items and HTTP status 200 (OK)
    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        try {
            List<Item> processedItems = itemService.processItemsAsync();
            return new ResponseEntity<>(processedItems, HttpStatus.OK);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process items", e);
        }
    }
}