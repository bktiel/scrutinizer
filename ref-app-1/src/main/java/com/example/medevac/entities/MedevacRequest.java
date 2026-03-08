package com.example.medevac.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import java.util.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MedevacRequest
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String location;
	private String callsign;
	private Integer patientnumber;
	private Integer precedence;
	@ManyToMany
	private List<SpecialEquipment> specialEquipment;
	private Integer litterpatient;
	private Integer ambulatorypatient;
	private Character security;
	@ElementCollection
	private List<String> markingMethod;
	private Integer nationality;
	private Integer nbc;
	private String status;
}
