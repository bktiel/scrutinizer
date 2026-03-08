package com.example.medevac.pojos;

import com.example.medevac.entities.MedevacRequest;
import com.example.medevac.repositories.SpecialEquipmentRepository;
import jakarta.validation.constraints.*;
import java.util.List;

public record MedevacRequestDto
(
	Long id,

	@NotNull
	@NotBlank
	@Pattern (regexp = "^\\d{1,2}\\s?[A-HJ-NP-Z]\\s?[A-HJ-NP-Z]{2}\\s?((\\d{10})|(\\d{8})|(\\d{5}\\s?\\d{5})|(\\d{4}\\s?\\d{4}))$", message = "Must provide a valid MGRS.")
	String location,

	@NotNull
	@NotBlank
	String callsign,

	@NotNull
	@Min(1)
	Integer patientnumber,

	@NotNull
	@Min(1)
	@Max(5)
	Integer precedence,

	@NotNull
	@Size (min = 1, max = 3)
	List<String> specialEquipment,

	@NotNull
	Integer litterpatient,

	@NotNull
	Integer ambulatorypatient,

	@NotNull
	Character security,

	@NotNull
	@Size (min = 1, max = 4)
	List<String> markingMethod,

	@NotNull
	@Min (1)
	@Max (5)
	Integer nationality,

	@NotNull
	@Min (1)
	@Max (4)
	Integer nbc,

	@NotNull
	@NotBlank
	String status
)
{
	public MedevacRequestDto (MedevacRequest req)
	{
		this (
			req.getId (),
			req.getLocation (),
			req.getCallsign (),
			req.getPatientnumber (),
			req.getPrecedence (),
			req.getSpecialEquipment ().stream ().map (equip -> equip.getSpecialEquipment ()).toList (),
			req.getLitterpatient (),
			req.getAmbulatorypatient (),
			req.getSecurity (),
			req.getMarkingMethod (),
			req.getNationality (),
			req.getNbc (),
			req.getStatus ()
		);
	}

	public static MedevacRequestDto from (MedevacRequest req)
	{
		return new MedevacRequestDto (req);
	}

	public MedevacRequest to (SpecialEquipmentRepository repo)
	{
		return new MedevacRequest (
			id,
			location,
			callsign,
			patientnumber,
			precedence,
			specialEquipment.stream ().map (repo::findBySpecialEquipment).toList (),
			litterpatient,
			ambulatorypatient,
			security,
			markingMethod,
			nationality,
			nbc,
			status
		);
	}
}
