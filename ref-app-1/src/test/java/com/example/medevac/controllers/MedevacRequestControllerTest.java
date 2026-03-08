package com.example.medevac.controllers;

import com.example.medevac.config.WebSecurityConfig;
import com.example.medevac.pojos.MedevacRequestDto;
import com.example.medevac.services.MedevacRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest (controllers = MedevacRequestController.class)
@AutoConfigureMockMvc
public class MedevacRequestControllerTest
{
	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mvc;

	@MockBean
	private MedevacRequestService service;

	private final ObjectMapper mapper = new ObjectMapper ();

	private final MedevacRequestDto requestDto0 = new MedevacRequestDto (
		null,
		"Location0",
		"Callsign0",
		3,
		1,
		List.of ("None"),
		2,
		1,
		'N',
		List.of ("Panel", "Pyro", "Smoke"),
		1,
		1,
		"Pending");

	private final MedevacRequestDto requestDto1 = new MedevacRequestDto (
		null,
		"Location1",
		"Callsign1",
		4,
		2,
		List.of ("Hoist"),
		3,
		1,
		'P',
		List.of ("None"),
		2,
		4,
		"Pending");

	@BeforeEach
	void init ()
	{
		mvc = MockMvcBuilders.webAppContextSetup (webApplicationContext).build ();
	}

//	Authentication getMockUserAuthentication (String... roles)
//	{
//		final var principal = mock (Principal.class);
//
//		when (principal.getName ()).thenReturn ("medevac");
//
//		final var authentication = mock (Authentication.class);
//		Collection<GrantedAuthority> authorities = Arrays.stream (roles).map (SimpleGrantedAuthority::new).collect (Collectors.toSet ());
//
//		when (authentication.getAuthorities ()).thenReturn (authorities);
//		when (authentication.getPrincipal ()).thenReturn (principal);
//
//		return authentication;
//	}

	@Test
	void shouldPostARequest () throws Exception
	{
		final var json = mapper.writeValueAsString (requestDto0);

		when (service.saveMedevacRequest (any (MedevacRequestDto.class))).thenReturn (requestDto0);

		mvc.perform (post ("/api/v1/medevac")
				.contentType (MediaType.APPLICATION_JSON)
				.accept (MediaType.APPLICATION_JSON)
				.content (json))
			.andExpect (status ().isCreated ())
			.andExpect (content ().json (json));

		var captor = ArgumentCaptor.forClass (MedevacRequestDto.class);

		verify (service).saveMedevacRequest (captor.capture ());
		assertThat (captor.getValue ()).usingRecursiveComparison ().isEqualTo (requestDto0);
	}

	@Test
	void shouldAcceptGetRequests () throws Exception
	{
		var expected = List.of (requestDto0, requestDto1);

		when (service.findAllMedevacRequests ()).thenReturn (expected);

		final var json = mapper.writeValueAsString (expected);

		mvc.perform (get ("/api/v1/medevac/all")
				.with(user ("Batman")
					.password ("Password")
					.authorities (new SimpleGrantedAuthority ("DISPATCHER"))))
			.andExpect (status ().isOk ())
			.andExpect (content ().json (json));
	}

	@Test
	void shouldAcceptPatchRequestsToUpdateNineLine () throws Exception
	{
		var original = List.of (requestDto0, requestDto1);
		var originalJson = mapper.writeValueAsString (original);

		final var updatedReq0 = new MedevacRequestDto (
			null,
			"Location0",
			"Callsign0",
			3,
			1,
			List.of ("None"),
			2,
			1,
			'N',
			List.of ("Panel", "Pyro", "Smoke"),
			1,
			1,
			"Complete");

		final var updatedReq1 = new MedevacRequestDto (
			null,
			"Location1",
			"Callsign1",
			4,
			2,
			List.of ("Hoist"),
			3,
			1,
			'P',
			List.of ("None"),
			2,
			4,
			"Complete");

		var updated = List.of (updatedReq0, updatedReq1);
		var updatedJson = mapper.writeValueAsString (List.of (updatedReq0, updatedReq1));

		when (service.setMedevacRequestsComplete (any (List.class))).thenReturn (updated);

		mvc.perform (patch ("/api/v1/medevac")
				.contentType (MediaType.APPLICATION_JSON)
				.accept (MediaType.APPLICATION_JSON)
				.content (originalJson))
			.andExpect (status ().isOk ())
			.andExpect (content ().json (updatedJson));

		var requestListCaptor = ArgumentCaptor.forClass (List.class);

		verify (service).setMedevacRequestsComplete (requestListCaptor.capture ());
		assertThat (requestListCaptor.getValue ()).usingRecursiveComparison ().isEqualTo (original);
	}
}
