package com.example.medevac.service;

import com.example.medevac.entities.MedevacRequest;
import com.example.medevac.entities.SpecialEquipment;
import com.example.medevac.pojos.MedevacRequestDto;
import com.example.medevac.repositories.MedevacRequestRepository;
import com.example.medevac.repositories.SpecialEquipmentRepository;
import com.example.medevac.services.MedevacRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@AutoConfigureMockMvc
public class MedevacRequestServiceTest
{
    @Mock
    private MedevacRequestRepository medevacRequestRepository;

    @Mock
    private SpecialEquipmentRepository specialEquipmentRepository;

    @InjectMocks
    private MedevacRequestService service;

    private SpecialEquipment equipment0 = new SpecialEquipment (1L, "None");
    private SpecialEquipment equipment1 = new SpecialEquipment (2L, "Hoist");

    private MedevacRequest request0;
    private MedevacRequest request1;

    private MedevacRequestDto requestDto0;
    private MedevacRequestDto requestDto1;

    @BeforeEach
    void setUp ()
    {
        MockitoAnnotations.openMocks (this);

        request0 = new MedevacRequest(
            1L,
            "56J MS 80443 25375",
            "Callsign0",
            3,
            1,
            List.of(equipment0),
            2,
            1,
            'N',
            List.of("Panel", "Pyro", "Smoke"),
            1,
            1,
            "Pending");

        request1 = new MedevacRequest(
            2L,
            "16S EG 89745 91523",
            "Callsign1",
            4,
            2,
            List.of(equipment1),
            3,
            1,
            'P',
            List.of("None"),
            2,
            4,
            "Pending");

        requestDto0 = MedevacRequestDto.from (request0);
        requestDto1 = MedevacRequestDto.from (request1);
    }

    @Test
    void shouldSaveSubmittedRequest () throws Exception
    {
        when (medevacRequestRepository.saveAndFlush (any (MedevacRequest.class))).thenReturn (request0);

        final var dto = MedevacRequestDto.from (request0);
        var result = service.saveMedevacRequest (dto);

        verify (medevacRequestRepository, times (1)).saveAndFlush (any (MedevacRequest.class));
        assertThat (result).usingRecursiveComparison ().isEqualTo (dto);
    }

    @Test
    void shouldUpdateCompletedRequest () throws Exception
    {
        final var originalDto = List.of (MedevacRequestDto.from (request0), MedevacRequestDto.from (request1));
        var updated = List.of (request0, request1);

        updated.get (0).setStatus ("Complete");
        updated.get (1).setStatus ("Complete");

        final var updatedDto = updated.stream ().map (MedevacRequestDto::from).toList ();

        when (specialEquipmentRepository.findBySpecialEquipment (any (String.class))).thenReturn (equipment0).thenReturn (equipment1);
        when (medevacRequestRepository.saveAllAndFlush (any (List.class))).thenReturn (updated);

        var result = service.setMedevacRequestsComplete (originalDto);
        var argCaptor = ArgumentCaptor.forClass (List.class);

        verify (medevacRequestRepository, times (1)).saveAllAndFlush (argCaptor.capture ());

        assertThat (argCaptor.getValue ()).usingRecursiveComparison ().isEqualTo (updated);
        assertThat (result).usingRecursiveComparison ().isEqualTo (updatedDto);
    }

    @Test
    void shouldDeleteRequestById () throws Exception
    {
        when (medevacRequestRepository.findById (anyLong ())).thenReturn (Optional.of (request0));
        doNothing ().when (medevacRequestRepository).deleteById (anyLong ());

        service.deleteMedevacRequestById (1L);

        verify (medevacRequestRepository, times(1)).deleteById (anyLong ());
    }

    @Test
    void shouldThrowWhenDeleteIsCalledWhenInvalidId () throws Exception
    {
        doNothing ().when (medevacRequestRepository).deleteById (anyLong ());

        assertThrows (ResponseStatusException.class, () -> service.deleteMedevacRequestById (-1L));
        assertThrows (ResponseStatusException.class, () -> service.deleteMedevacRequestById (0L));

        when (medevacRequestRepository.findById (anyLong ())).thenReturn (Optional.empty ());

        final var exception = assertThrows (ResponseStatusException.class, () -> service.deleteMedevacRequestById (1L));

        assertEquals ("No such request found.", exception.getReason ());
    }

    @Test
    void shouldDeleteRequest () throws Exception
    {
        when (medevacRequestRepository.findById (anyLong ())).thenReturn (Optional.of (request0));
        doNothing ().when (medevacRequestRepository).delete (any (MedevacRequest.class));

        service.deleteMedevacRequest (requestDto0);

        verify (medevacRequestRepository, times(1)).delete (any (MedevacRequest.class));
    }

    @Test
    void shouldRejectInvalidDtoForDeletion () throws Exception
    {
        doNothing ().when (medevacRequestRepository).delete (any (MedevacRequest.class));
        when (medevacRequestRepository.findById (anyLong ())).thenReturn (Optional.empty ());

        final var exception = assertThrows (ResponseStatusException.class, () -> service.deleteMedevacRequest (requestDto0));

        assertEquals ("No such request found.", exception.getReason ());
    }

    @Test
    void shouldRejectInvalidRequestDto () throws Exception
    {
        final var request = new MedevacRequestDto (
            null,
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

        when (medevacRequestRepository.saveAndFlush (any (MedevacRequest.class))).thenThrow (new RuntimeException ("This should not happen."));

        final var exception = assertThrows (ResponseStatusException.class, () -> service.saveMedevacRequest (request));

        assertEquals (HttpStatus.BAD_REQUEST, exception.getStatusCode ());
        assertThat (exception.getReason ()).contains ("Must provide a valid MGRS.");
    }
}
