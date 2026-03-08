package com.example.medevac.pojos;

import com.example.medevac.entities.MedevacRequest;
import jakarta.persistence.*;

public record AssignmentId(
	@Id
	@OneToOne (fetch = FetchType.LAZY)
	@JoinColumn (name = "request_id")
	MedevacRequest request,
	@Id
	Long responder)
{
}
