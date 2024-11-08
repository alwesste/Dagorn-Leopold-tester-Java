package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)

public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(ticketDAO.getNbTicket(anyString())).thenReturn(1);
            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void testProcessIncomingVehicle() {
        // arrange
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        // act
        parkingService.processIncomingVehicle();
        // assert
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() {
        // arrange
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);      
        // act
        parkingService.processExitingVehicle();
        // assert
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // En option pour tester que ce updtaeParking n'est jamais appeler !
    }

    @Test
    public void testGetNextParkingNumberIfAvailable() {
        // arrange
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        // act 
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();
        // assert 
        assertEquals(  1, spot.getId());
        assertEquals(true, spot.isAvailable());
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
        //arrange
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);
        //act
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();
        //assert
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertNull(spot, "Error fetching parking number from DB. Parking slots might be full");
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument  () {
        //arrange
        when(inputReaderUtil.readSelection()).thenReturn(3);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        //act
        ParkingSpot spot = parkingService.getNextParkingNumberIfAvailable();
        //assert
        verify(inputReaderUtil, Mockito.times(1)).readSelection();
        assertNull(spot, "Error parsing user input for type of vehicle");
    }

    @Test
    public void processExitingVehicleTest() {
        // Arrange 
        // Configure le comportement du mock tiketDAO pour la methode getNbTicket()
        when(ticketDAO.getNbTicket(anyString())).thenReturn(1);

        // ACT
        // Gere la sortie du vehicule
        parkingService.processExitingVehicle();

        // Assert
        // Verifie la methode updateParking quand n'importe quel vehicule sort.
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        // Verifie la methode getNbTicket avec la plaque "ABSCDEF"
        verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF");
    }
}