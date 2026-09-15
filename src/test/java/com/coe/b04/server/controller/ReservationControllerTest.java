package com.coe.b04.server.controller;

import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.io.ReservationResponse;
import com.coe.b04.server.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerTest {

    private MockMvc mockMvc;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservationController(reservationService)).build();
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(reservationService.create(any())).thenReturn(ReservationResponse.builder()
                .reservationNumber("RES-20260914-AB12CD")
                .status(ReservationStatus.PENDING)
                .build());

        mockMvc.perform(post("/reservation/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"Frankfurt am Main","destination":"Madrid",
                                 "departureDate":"2026-10-10","adults":2,
                                 "hotelId":"HOT-1001","roomId":"ROOM-102","currency":"EUR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/reservation/snapshot?reservationNumber=RES-20260914-AB12CD"))
                .andExpect(jsonPath("$.reservationNumber").value("RES-20260914-AB12CD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createReturns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/reservation/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    void createReturns400WhenServiceRejects() throws Exception {
        when(reservationService.create(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one of flightId, hotelId or carId is required"));

        mockMvc.perform(post("/reservation/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"Frankfurt am Main","destination":"Madrid",
                                 "departureDate":"2026-10-10","adults":1,"currency":"EUR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturns200() throws Exception {
        when(reservationService.getByNumber("RES-X")).thenReturn(ReservationResponse.builder()
                .reservationNumber("RES-X")
                .status(ReservationStatus.PENDING)
                .build());

        mockMvc.perform(get("/reservation/snapshot")
                        .param("reservationNumber", "RES-X"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationNumber").value("RES-X"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getReturns400WhenReservationNumberMissing() throws Exception {
        mockMvc.perform(get("/reservation/snapshot"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    void getUnknownReturns404() throws Exception {
        when(reservationService.getByNumber("RES-UNKNOWN"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/reservation/snapshot")
                        .param("reservationNumber", "RES-UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelReturns200() throws Exception {
        when(reservationService.cancel("RES-X")).thenReturn(ReservationResponse.builder()
                .reservationNumber("RES-X")
                .status(ReservationStatus.CANCELLED)
                .build());

        mockMvc.perform(post("/reservation/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationNumber\":\"RES-X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelReturns400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/reservation/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    void cancelConflictReturns409() throws Exception {
        when(reservationService.cancel("RES-X"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Reservation cannot be cancelled"));

        mockMvc.perform(post("/reservation/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationNumber\":\"RES-X\"}"))
                .andExpect(status().isConflict());
    }
}
