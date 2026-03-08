package com.example.medevac.service;

import com.example.medevac.entities.Assignment;
import com.example.medevac.entities.MedevacRequest;
import com.example.medevac.pojos.AssignmentDto;
import com.example.medevac.pojos.MedevacRequestDto;
import com.example.medevac.repositories.AssignmentRepository;
import com.example.medevac.repositories.SpecialEquipmentRepository;
import com.example.medevac.services.AssignmentService;
import com.example.medevac.services.MedevacRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
public class AssignmentServiceTest
{
	@Mock
	private AssignmentRepository repo;

	@InjectMocks
	private AssignmentService service;

	@Mock
	private MedevacRequestService medevacRequestService;

	@Mock
	private SpecialEquipmentRepository equipmentRepo;

	private final MedevacRequestDto requestDto0 = new MedevacRequestDto (
		1L,
		"Location0",
		"Callsign0",
		3,
		1,
		List.of("None"),
		2,
		1,
		'N',
		List.of("Panel", "Pyro", "Smoke"),
		1,
		1,
		"Pending");

	private final MedevacRequestDto requestDto1 = new MedevacRequestDto (
		2L,
		"Location1",
		"Callsign1",
		4,
		2,
		List.of("Hoist"),
		3,
		1,
		'P',
		List.of("None"),
		2,
		4,
		"Pending");

	private final MedevacRequest request0 = new MedevacRequest(
		1L,
		"Location0",
		"Callsign0",
		3,
		1,
		null,
		2,
		1,
		'N',
		List.of("Panel", "Pyro", "Smoke"),
		1,
		1,
		"Pending");

	private final MedevacRequest request1 = new MedevacRequest(
		2L,
		"Location1",
		"Callsign1",
		4,
		2,
		null,
		3,
		1,
		'P',
		List.of("None"),
		2,
		4,
		"Pending");

	private final AssignmentDto assignDto0 = new AssignmentDto (1L, 1L);
	private final AssignmentDto assignDto1 = new AssignmentDto (2L, 2L);

	private Assignment assign0;
	private Assignment assign1;

	@BeforeEach
	void setUp ()
	{
		MockitoAnnotations.openMocks (this);

		service.setMedevacRequestService (medevacRequestService);

		assign0 = new Assignment (requestDto0.to (equipmentRepo), assignDto0.responder ());
		assign1 = new Assignment (requestDto1.to (equipmentRepo), assignDto1.responder ());
	}

	@Test
	void shouldSaveNewAssignment () throws Exception
	{
		when (medevacRequestService.findMedevacRequestById (any (Long.class))).thenReturn (Optional.of (requestDto0)).thenReturn (Optional.of (requestDto1));
		when (repo.saveAndFlush (any (Assignment.class))).thenReturn (assign0).thenReturn (assign1);

		var result = service.saveAssignment (assignDto0);

		verify (repo, times (1)).saveAndFlush (any (Assignment.class));
		assertThat (result).usingRecursiveComparison ().isEqualTo (assignDto0);

		result = service.saveAssignment (assignDto1);

		verify (repo, times (2)).saveAndFlush (any (Assignment.class));
		assertThat (result).usingRecursiveComparison ().isEqualTo (assignDto1);
	}

	@Test
	void shouldSaveNewAssignments () throws Exception
	{
		final var assignments = List.of (assign0, assign1);
		final var dtos = List.of (assignDto0, assignDto1);

		when (medevacRequestService.findRealMedevacRequestById (anyLong ())).thenReturn (Optional.of (request0)).thenReturn (Optional.of (request1));
		when (repo.saveAllAndFlush (any (List.class))).thenReturn (assignments);

		var result = service.saveAllAssignments (dtos);

		verify (repo, times (1)).saveAllAndFlush (any (List.class));
		assertThat (result).usingRecursiveComparison ().isEqualTo (dtos);
	}

	@Test
	void shouldGetAllAssignments () throws Exception
	{
		final var assignments = List.of (assign0, assign1);
		final var assignDtos = List.of (assignDto0, assignDto1);

		when (medevacRequestService.findMedevacRequestById (any (Long.class))).thenReturn (Optional.of (requestDto0)).thenReturn (Optional.of (requestDto1));
		when (repo.findAll ()).thenReturn (assignments);

		assertThat (service.findAllAssignments ()).isEqualTo (assignDtos).usingRecursiveComparison ();
	}

	@Test
	void shouldRejectAssignmentWithInvalidValues () throws Exception
	{
		final var expectedErrorMessage = "Invalid id value.";
		final var assignment = new AssignmentDto (-1, -1);

		when (repo.saveAndFlush (any (Assignment.class))).thenThrow (RuntimeException.class); // should not happen.

		final var exception = assertThrows (ResponseStatusException.class, () -> service.saveAssignment (assignment));

		assertEquals (expectedErrorMessage, exception.getReason ());
	}

	@Test
	void shouldRejectAssignmentWithNonExistingRequest () throws Exception
	{
		final var errorMessage = "Request not found.";

		when (medevacRequestService.findMedevacRequestById (any (Long.class))).thenReturn (Optional.empty ());
		when (repo.saveAndFlush (any (Assignment.class))).thenThrow (RuntimeException.class); // should not happen.

		final var exception = assertThrows (ResponseStatusException.class, () -> service.saveAssignment (assignDto0));

		assertEquals (errorMessage, exception.getReason ());
	}

	@Test
	void shouldRejectAssignmentsWithSameRequestId () throws Exception
	{
		when (medevacRequestService.findMedevacRequestById (any (Long.class))).thenReturn (Optional.of (requestDto0)).thenReturn (Optional.of (requestDto1));
		when (repo.saveAllAndFlush (any (List.class))).thenReturn (null);

		assertThrows (ResponseStatusException.class, () -> service.saveAllAssignments (List.of (assignDto0, assignDto0)));
		verify (repo, times (0)).saveAllAndFlush (any (List.class));
	}

	@Test
	void shouldRejectAssignmentsWithInvalidValues () throws Exception
	{
		final var expectedErrorMessage = "Invalid id value.";
		final var assignments = List.of (new AssignmentDto (-1, -1), new AssignmentDto (-2, 3));

		when (repo.saveAllAndFlush (any (List.class))).thenThrow (RuntimeException.class); // should not happen.

		final var exception = assertThrows (ResponseStatusException.class, () -> service.saveAllAssignments (assignments));

		assertEquals (expectedErrorMessage, exception.getReason ());
	}

	@Test
	void shouldRejectAssignmentsWithNonExistingRequest () throws Exception
	{
		final var errorMessage = "Request not found.";
		final var assignments = List.of (assignDto0, assignDto1);

		when (medevacRequestService.findMedevacRequestById (any (Long.class))).thenReturn (Optional.empty ());
		when (repo.saveAllAndFlush (any (List.class))).thenThrow (RuntimeException.class); // should not happen.

		final var exception = assertThrows (ResponseStatusException.class, () -> service.saveAllAssignments (assignments));

		assertEquals (errorMessage, exception.getReason ());
	}
}
