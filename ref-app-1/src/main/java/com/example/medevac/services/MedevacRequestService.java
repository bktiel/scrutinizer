package com.example.medevac.services;

import com.example.medevac.entities.Assignment;
import com.example.medevac.entities.MedevacRequest;
import com.example.medevac.entities.NineLineUserDetails;
import com.example.medevac.pojos.MedevacRequestDto;
import com.example.medevac.repositories.AssignmentRepository;
import com.example.medevac.repositories.MedevacRequestRepository;
import com.example.medevac.repositories.SpecialEquipmentRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Validation;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MedevacRequestService
{
	private final MedevacRequestRepository repo;
	private final SpecialEquipmentRepository equipmentRepo;
	private final AssignmentRepository assignmentRepository;

	public MedevacRequestService (MedevacRequestRepository repo, SpecialEquipmentRepository equipmentRepo, AssignmentRepository assignmentRepository)
	{
		this.repo = repo;
		this.equipmentRepo = equipmentRepo;
		this.assignmentRepository = assignmentRepository;
	}

	public Optional<MedevacRequest> findRealMedevacRequestById (long id)
	{
		return repo.findById (id);
	}

	public Optional<MedevacRequestDto> findMedevacRequestById (long id)
	{
		var opt = repo.findById (id);

		return opt.map (MedevacRequestDto::from);
	}

	@Transactional
	public List<MedevacRequestDto> findResponderMedevacRequests ()
	{
		final var auth = SecurityContextHolder.getContext ().getAuthentication ();

		if (auth.getAuthorities ().contains (new SimpleGrantedAuthority ("RESPONDER")))
		{
			var assignments = assignmentRepository.findByResponder (((NineLineUserDetails) auth.getPrincipal ()).getId ());
			var entities = assignments.stream ().map (Assignment::getRequest).toList ();
			var dtos = new ArrayList<MedevacRequestDto> ();

			for (var entity : entities)
			{
				dtos.add (MedevacRequestDto.from (entity));
			}
			return dtos;
		}

		throw new ResponseStatusException (HttpStatus.UNAUTHORIZED, "Access Denied.");
	}

	@Transactional
	public List<MedevacRequestDto> findAllMedevacRequests ()
	{
		final var auth = SecurityContextHolder.getContext ().getAuthentication ();

		if (auth.getAuthorities ().contains (new SimpleGrantedAuthority ("DISPATCHER")))
		{
			var entities = repo.findAll ();
			var dtos = new ArrayList<MedevacRequestDto> ();

			for (var entity : entities)
			{
				dtos.add (MedevacRequestDto.from (entity));
			}

			return dtos;
		}

		throw new ResponseStatusException (HttpStatus.UNAUTHORIZED, "Access Denied.");
	}

	@Transactional
	public MedevacRequestDto saveMedevacRequest (MedevacRequestDto dto)
	{
		try (var factory = Validation.buildDefaultValidatorFactory ())
		{
			var validator = factory.getValidator ();
			var violations = validator.validate (dto);

			if (! violations.isEmpty ())
			{
				throw new ResponseStatusException (HttpStatus.BAD_REQUEST, violations.toString ());
			}
		}

		return new MedevacRequestDto (repo.saveAndFlush (dto.to (equipmentRepo)));
	}

	public void deleteMedevacRequestById (long id)
	{
		if (id < 1L)
		{
			throw new ResponseStatusException (HttpStatus.BAD_REQUEST, "Invalid id value.");
		}

		final var requestOptional = repo.findById (id);

		if (requestOptional.isEmpty ())
		{
			throw new ResponseStatusException (HttpStatus.BAD_REQUEST, "No such request found.");
		}

		repo.deleteById (id);
	}

	public void deleteMedevacRequest (MedevacRequestDto medevacRequest)
	{
		final var requestOptional = repo.findById (medevacRequest.id ());

		if (requestOptional.isEmpty ())
		{
			throw new ResponseStatusException (HttpStatus.BAD_REQUEST, "No such request found.");
		}

		repo.delete (medevacRequest.to (equipmentRepo));
	}

	@Transactional
	public List<MedevacRequestDto> setMedevacRequestsComplete (List<MedevacRequestDto> requests)
	{
		var entities = new ArrayList<MedevacRequest> ();

		for (var request : requests)
		{
			var entity = request.to (equipmentRepo);

			entity.setStatus ("Complete");
			entities.add (entity);
		}

		return repo.saveAllAndFlush (entities).stream ().map (MedevacRequestDto::from).toList ();
	}
}
