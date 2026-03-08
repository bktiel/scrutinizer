package com.example.medevac.controllers;

import com.example.medevac.pojos.ApiErrorDto;
import com.example.medevac.pojos.AssignmentDto;
import com.example.medevac.pojos.MedevacRequestDto;
import com.example.medevac.services.AssignmentService;
import com.example.medevac.services.NineLineUserDetailsService;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssignmentController.class)
@AutoConfigureMockMvc
public class AssignmentControllerTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mvc;

    @MockBean
    private AssignmentService service;

    @MockBean
    private NineLineUserDetailsService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    private final MedevacRequestDto request0 = new MedevacRequestDto(
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

    private final MedevacRequestDto request1 = new MedevacRequestDto(
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

    @BeforeEach
    void init ()
    {
        mvc = MockMvcBuilders.webAppContextSetup (webApplicationContext).build ();
    }

    @Test
    void shouldGetAllAssignments () throws Exception
    {
        var assignments = List.of (new AssignmentDto (request0.id (), 1L), new AssignmentDto (request1.id (), 1L));

        when (service.saveAllAssignments (any (List.class))).thenReturn (assignments);

        mvc.perform (post ("/api/v1/assignment")
                .contentType (MediaType.APPLICATION_JSON)
                .accept (MediaType.APPLICATION_JSON)
                .content (mapper.writeValueAsString (assignments)))
            .andExpect (status ().isCreated ())
            .andExpect (content ().json (mapper.writeValueAsString (assignments), true));

        var captor = ArgumentCaptor.forClass (List.class);

        verify (service).saveAllAssignments (captor.capture ());
        assertThat (captor.getValue ()).usingRecursiveComparison ().isEqualTo (assignments);
    }

    @Test
    void shouldRejectRequestWithSameId () throws Exception
    {
        final var errorMessage = "Duplicated values.";
        final var expected = new ApiErrorDto (HttpStatus.CONFLICT.value (), errorMessage);
        final var assignments = List.of (new AssignmentDto (request0.id (), 1L), new AssignmentDto (request0.id (), 2L));

        when (service.saveAllAssignments (any (List.class))).thenThrow (new ResponseStatusException (HttpStatus.CONFLICT, errorMessage));

        mvc.perform (post ("/api/v1/assignment")
                .contentType (MediaType.APPLICATION_JSON)
                .accept (MediaType.APPLICATION_JSON)
                .content (mapper.writeValueAsString (assignments)))
            .andExpect (status ().isConflict ())
            .andExpect (content ().json (mapper.writeValueAsString (expected), true));

        var captor = ArgumentCaptor.forClass (List.class);

        verify (service).saveAllAssignments (captor.capture ());
        assertThat (captor.getValue ()).usingRecursiveComparison ().isEqualTo (assignments);
    }

    @Test
    void shouldGetAllAssignment () throws Exception
    {
        var assignments = List.of (new AssignmentDto (request0.id (), 1L), new AssignmentDto (request0.id (), 2L));

        when (service.findAllAssignments ()).thenReturn (assignments);

        mvc.perform (get ("/api/v1/assignment")
                .accept (MediaType.APPLICATION_JSON))
            .andExpect (status ().isOk ())
            .andExpect (content ().json (mapper.writeValueAsString (assignments), true));
    }
}
