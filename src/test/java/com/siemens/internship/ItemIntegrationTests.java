package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ItemIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    public void cleanup() {
        itemRepository.deleteAll();
    }

    @Test
    public void testCreateItemAndGetById() throws Exception {

        // Create a new item
        Item newItem = new Item();
        newItem.setName("Integration Test Item");
        newItem.setDescription("Created during integration test");
        newItem.setEmail("test@example.com");

        // Perform POST request to create the item
        String location = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Integration Test Item"))
                .andReturn().getResponse().getHeader("Location");

        // Extract the ID from the Location header or response body
        String responseBody = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andReturn().getResponse().getContentAsString();

        Item createdItem = objectMapper.readValue(responseBody, Item.class);
        Long itemId = createdItem.getId();

        // Perform GET request to retrieve the created item
        mockMvc.perform(get("/api/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Integration Test Item"))
                .andExpect(jsonPath("$.description").value("Created during integration test"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testGetAllItemsWhenEmpty() throws Exception {
        // Ensure repository is empty
        itemRepository.deleteAll();

        // Perform GET request and verify empty array
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testValidationFailure() throws Exception {
        // Create item with invalid data
        Item invalidItem = new Item();
        invalidItem.setName(""); // Empty name
        invalidItem.setEmail("invalid-email"); // Invalid email

        // Perform POST request and verify validation failure
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testProcessItems() throws Exception {
        // Create and save test items
        Item item1 = new Item();
        item1.setName("Process Item 1");
        item1.setStatus("NEW");
        itemRepository.save(item1);

        Item item2 = new Item();
        item2.setName("Process Item 2");
        item2.setStatus("NEW");
        itemRepository.save(item2);

        // Perform GET request to process items
        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].status").value("PROCESSED"));
    }

    @Test
    public void testUpdateNonExistentItem() throws Exception {
        // Create item with non-existent ID
        Item nonExistentItem = new Item();
        nonExistentItem.setId(999L); // ID that doesn't exist
        nonExistentItem.setName("Non-existent Item");
        nonExistentItem.setEmail("test@example.com");

        // Perform PUT request and verify 404 response
        mockMvc.perform(put("/api/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentItem)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteNonExistentItem() throws Exception {
        // Perform DELETE request for non-existent ID
        mockMvc.perform(delete("/api/items/999"))
                .andExpect(status().isNotFound());
    }
}