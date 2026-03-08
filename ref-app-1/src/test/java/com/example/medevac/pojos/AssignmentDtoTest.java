package com.example.medevac.pojos;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class AssignmentDtoTest
{
	@Test
	void shouldRejectNegativeAndZeroValues () throws Exception
	{
		try (var factory = Validation.buildDefaultValidatorFactory ())
		{
			var validator = factory.getValidator ();
			var assign = new AssignmentDto (- 1, 0);
			var violations = validator.validate (assign);

			assertThat (violations).hasSize (2);
		}
	}
}
