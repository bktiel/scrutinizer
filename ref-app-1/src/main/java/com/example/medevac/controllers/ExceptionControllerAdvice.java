package com.example.medevac.controllers;

import com.example.medevac.pojos.ApiErrorDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class ExceptionControllerAdvice
{
	@ExceptionHandler
	protected ResponseEntity<ApiErrorDto> handleResponseException (ResponseStatusException exception)
	{
		return ResponseEntity
			.status (exception.getStatusCode ())
			.contentType (MediaType.APPLICATION_JSON)
			.body (new ApiErrorDto (exception.getStatusCode ().value (), exception.getReason ()));
	}
}
