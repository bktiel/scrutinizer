package com.example.medevac.controllers;

import com.example.medevac.entities.MedevacRequest;
import com.example.medevac.pojos.MedevacRequestDto;
import com.example.medevac.services.MedevacRequestService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping ("/api/v1")
public class MedevacRequestController
{
	private final MedevacRequestService service;

	public MedevacRequestController (MedevacRequestService service)
	{
		this.service = service;
	}

	@PostMapping ("/medevac")
	public ResponseEntity<MedevacRequestDto> createMedevacRequest (@RequestBody MedevacRequestDto medevacRequest)
	{
		MedevacRequestDto createdRequest = service.saveMedevacRequest (medevacRequest);
		return new ResponseEntity<> (createdRequest, HttpStatus.CREATED);
	}

	@GetMapping ("/medevac")
	public ResponseEntity<List<MedevacRequestDto>> getResponderMedevacRequests ()
	{
		return new ResponseEntity<> (service.findResponderMedevacRequests(), HttpStatus.OK);
	}

	@GetMapping ("/medevac/all")
	public ResponseEntity<List<MedevacRequestDto>> getAllMedevacRequests ()
	{
		return new ResponseEntity<> (service.findAllMedevacRequests(), HttpStatus.OK);
	}

	@PatchMapping ("/medevac")
	public ResponseEntity<List<MedevacRequestDto>> updateMedevacRequest (@RequestBody List<MedevacRequestDto> listOfMedevacRequests)
	{
		return new ResponseEntity<> (service.setMedevacRequestsComplete (listOfMedevacRequests), HttpStatus.OK);
	}
}
