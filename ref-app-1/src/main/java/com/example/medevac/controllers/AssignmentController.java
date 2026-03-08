package com.example.medevac.controllers;

import java.util.*;

import com.example.medevac.entities.NineLineUser;
import com.example.medevac.pojos.AssignmentDto;
import com.example.medevac.pojos.ResponderDto;
import com.example.medevac.services.AssignmentService;
import com.example.medevac.services.NineLineUserDetailsService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping ("/api/v1")
public class AssignmentController
{
	private final AssignmentService service;
	private final NineLineUserDetailsService nineLineUserDetailsService;

	public AssignmentController (AssignmentService service, NineLineUserDetailsService nineLineUserDetailsService)
	{
		this.service = service;
		this.nineLineUserDetailsService = nineLineUserDetailsService;
	}

	@PostMapping ("/assignment")
	@ResponseStatus (HttpStatus.CREATED)
	public List<AssignmentDto> createAssignments (@RequestBody List<AssignmentDto> assignments)
	{
		return service.saveAllAssignments (assignments);
	}

	@GetMapping ("/assignment")
	@ResponseStatus (HttpStatus.OK)
	public List<AssignmentDto> getAllAssignments ()
	{
		return service.findAllAssignments();
	}

	@GetMapping("/assignment/responders")
	@ResponseStatus(HttpStatus.OK)
	public List<ResponderDto> getAllResponders () {
		return nineLineUserDetailsService.getAllResponders();
	}
}
