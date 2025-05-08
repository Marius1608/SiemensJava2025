package com.siemens.internship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import com.siemens.internship.validator.EmailValidator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternshipApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private ItemController itemController;

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemRepository itemRepository;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TestRestTemplate restTemplate;

	@Mock
	private ItemRepository mockItemRepository;

	@InjectMocks
	private ItemService mockItemService;


	@AfterEach
	public void cleanup() {
		itemRepository.deleteAll();
	}

	@Test
	void contextLoads() {
		assertThat(context).isNotNull();
	}

	@Test
	void controllerLoads() {
		assertThat(itemController).isNotNull();
	}

	@Test
	void serviceLoads() {
		assertThat(itemService).isNotNull();
	}

	@Test
	void repositoryLoads() {
		assertThat(itemRepository).isNotNull();
	}

	@Test
	void emailValidatorWorks() {
		assertThat(EmailValidator.isValid("test123@gmail.com")).isTrue();
		assertThat(EmailValidator.isValid("email-email")).isFalse();
	}

	@Test
	public void testCreateItemAndGetById() throws Exception {

		// Create a new item
		Item newItem = new Item();
		newItem.setName("Test Item");
		newItem.setDescription("Integration test");
		newItem.setEmail("test123@gmail.com");

		// Perform POST request to create the item
		String location = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(newItem)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.name").value("Test Item"))
				.andReturn().getResponse().getHeader("Location");

		// Extract the id from the Location header or response body
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
				.andExpect(jsonPath("$.name").value("Test Item"))
				.andExpect(jsonPath("$.description").value("Integration test"))
				.andExpect(jsonPath("$.email").value("test123@gmail.com"));
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
		invalidItem.setEmail("email-email"); // Invalid email

		// Perform POST request and verify validation failure
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidItem)))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testUpdateNonExistentItem() throws Exception {
		// Create item with non-existent ID
		Item nonExistentItem = new Item();
		nonExistentItem.setId(999L); // ID that doesn't exist
		nonExistentItem.setName("Non item");
		nonExistentItem.setEmail("test123@gmail.com");

		// Perform PUT request and verify 404 response
		mockMvc.perform(put("/api/items/999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(nonExistentItem)))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testDeleteNonExistentItem() throws Exception {
		// Perform DELETE request for non-existent if
		mockMvc.perform(delete("/api/items/999")).andExpect(status().isNotFound());
	}

	@Test
	void getItemById_success() {
		Item item = new Item(
				null,
				"Product Sample",
				"High-quality testing product",
				"NEW",
				"contact@siemens.com");
		ResponseEntity<Item> createResponse = restTemplate.postForEntity("/api/items", item, Item.class);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Item createdItem = createResponse.getBody();
		assertThat(createdItem).isNotNull();

		ResponseEntity<Item> getResponse = restTemplate.getForEntity("/api/items/" + createdItem.getId(), Item.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getResponse.getBody()).isNotNull();
		assertThat(getResponse.getBody().getId()).isEqualTo(createdItem.getId());
		assertThat(getResponse.getBody().getName()).isEqualTo("Product Sample");
		assertThat(getResponse.getBody().getDescription()).isEqualTo("High-quality testing product");
		assertThat(getResponse.getBody().getStatus()).isEqualTo("NEW");
		assertThat(getResponse.getBody().getEmail()).isEqualTo("contact@siemens.com");
	}

	@Test
	void getItemById_notFound() {
		ResponseEntity<Item> response = restTemplate.getForEntity("/api/items/12345", Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updateItem_success() {
		Item original = new Item(
				null,
				"Industrial Sensor",
				"Temperature sensor",
				"AVAILABLE",
				"support@siemens.com");
		ResponseEntity<Item> postResponse = restTemplate.postForEntity("/api/items", original, Item.class);

		assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long id = Objects.requireNonNull(postResponse.getBody()).getId();

		Item updated = new Item(
				null,
				"Industrial Sensor Pro",
				"Advanced temperature sensor with WiFi",
				"FEATURED",
				"enterprise@siemens.com");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Item> request = new HttpEntity<>(updated, headers);

		ResponseEntity<Item> putResponse = restTemplate.exchange("/api/items/" + id, HttpMethod.PUT, request, Item.class);

		assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(putResponse.getBody()).isNotNull();
		assertThat(putResponse.getBody().getId()).isEqualTo(id);
		assertThat(putResponse.getBody().getName()).isEqualTo("Industrial Sensor Pro");
		assertThat(putResponse.getBody().getDescription()).isEqualTo("Advanced temperature sensor with WiFi");
		assertThat(putResponse.getBody().getStatus()).isEqualTo("FEATURED");
		assertThat(putResponse.getBody().getEmail()).isEqualTo("enterprise@siemens.com");
	}

	@Test
	void updateItem_notFound() {
		Item item = new Item(
				null,
				"Non-existent Product",
				"This product doesn't exist",
				"UNAVAILABLE",
				"info@siemens.com");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Item> request = new HttpEntity<>(item, headers);

		ResponseEntity<Item> response = restTemplate.exchange("/api/items/54321", HttpMethod.PUT, request, Item.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteItem_success() {
		Item item = new Item(
				null,
				"Obsolete Component",
				"Legacy system component",
				"DISCONTINUED",
				"archive@siemens.com");
		ResponseEntity<Item> postResponse = restTemplate.postForEntity("/api/items", item, Item.class);

		assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long id = Objects.requireNonNull(postResponse.getBody()).getId();

		ResponseEntity<Void> deleteResponse = restTemplate.exchange("/api/items/" + id, HttpMethod.DELETE, null, Void.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<Item> getResponse = restTemplate.getForEntity("/api/items/" + id, Item.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void deleteItem_notFound() {
		ResponseEntity<Void> response = restTemplate.exchange("/api/items/98765", HttpMethod.DELETE, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

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
		when(mockItemRepository.findAllIds()).thenReturn(Arrays.asList(id1, id2));
		when(mockItemRepository.findById(id1)).thenReturn(Optional.of(item1));
		when(mockItemRepository.findById(id2)).thenReturn(Optional.of(item2));
		when(mockItemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

		// Configure service to use synchronous executor for testing
		TaskExecutor syncExecutor = new SyncTaskExecutor();
		ReflectionTestUtils.setField(mockItemService, "taskExecutor", syncExecutor);

		// Execute the method
		List<Item> processedItems = mockItemService.processItemsAsync();

		// Verify results
		assertThat(processedItems).hasSize(2);
		assertThat(processedItems.get(0).getStatus()).isEqualTo("PROCESSED");
		assertThat(processedItems.get(1).getStatus()).isEqualTo("PROCESSED");

		// Verify interactions
		verify(mockItemRepository, times(1)).findAllIds();
		verify(mockItemRepository, times(1)).findById(id1);
		verify(mockItemRepository, times(1)).findById(id2);
		verify(mockItemRepository, times(2)).save(any(Item.class));
	}
}