package com.example.medevac.pojos;

import com.example.medevac.entities.Assignment;
import com.example.medevac.entities.MedevacRequest;
import com.example.medevac.repositories.SpecialEquipmentRepository;
import jakarta.validation.constraints.*;

public record AssignmentDto(
	@Min (1)
	long requestId,

	@Min (1)
	long responder)
{
	public AssignmentDto (Assignment assignment)
	{
		this (assignment.getRequest ().getId (), assignment.getResponder ());
	}

	public static AssignmentDto from (Assignment assignment)
	{
		return new AssignmentDto (assignment.getRequest ().getId (), assignment.getResponder ());
	}

	public Assignment to (MedevacRequestDto dto, SpecialEquipmentRepository repo)
	{
		return new Assignment (dto.to (repo), responder);
	}

	public Assignment to (MedevacRequest request, SpecialEquipmentRepository repo)
	{
		return new Assignment (request, responder);
	}
}
