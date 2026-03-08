package com.example.medevac.pojos;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class MedevacRequestDtoTest
{
	/*
		var request = new MedevacRequestDto (
			1L,
			"56J MS 80443 25375",
			"Javascript",
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
	 */

	private void runValidation (MedevacRequestDto dto, int errors)
	{
		try (var factory = Validation.buildDefaultValidatorFactory ())
		{
			var validator = factory.getValidator ();
			var violations = validator.validate (dto);

			assertThat (violations).hasSize (errors);
		}
	}

	@Test
	void shouldAcceptDto ()
	{
		var request = new MedevacRequestDto (
			1L,
			"56J MS 80443 25375",
			"Javascript",
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

		runValidation (request, 0);
	}

	@Test
	void shouldRejectMalformedRequest ()
	{
		var request = new MedevacRequestDto (
			1L,
			"Javascript",
			"",
			0,
			6,
			null,
			null,
			null,
			null,
			List.of (),
			8,
			-1,
			"");

		runValidation (request, 12);
	}
}
