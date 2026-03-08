package com.example.medevac.entities;

import com.example.medevac.pojos.AssignmentId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@IdClass (AssignmentId.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Assignment
{
	@Id
	@OneToOne (fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
	@JoinColumn (name = "request_id")
	private MedevacRequest request;

	@Id
	private Long responder;
}
