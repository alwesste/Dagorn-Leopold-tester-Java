package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// import java.sql.Date;
import java.util.Date;


@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    private static Date inTime;
    private static Date outTime;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
        outTime = new Date();
        inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar(){
        // arrange
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // act
        parkingService.processIncomingVehicle(inTime);

        // assert
        // Check that a ticket is actualy saved in DB
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Ticket n'est pas null dans la base de donnee");
        System.out.println("Le ticket de testPArkingCar() pour le registration est " + ticket.getVehicleRegNumber());

        // Parking table is updated with availability
        // int parkingSpotId = ticket.getParkingSpot().getId();
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        boolean isUpdated = parkingSpotDAO.updateParking(parkingSpot);
        assertTrue(isUpdated, "La place de parking a ete mise a jour");
        System.out.println("Le parking est mis a jour " + isUpdated);      
    }

    @Test
    public void testParkingLotExit(){
        // arrange
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processExitingVehicle(outTime);
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        // assert
        // check that the fare generated and out time are populated correctly in the database
        assertNotNull(ticket.getOutTime(), "L'heure est correctement injecter dana la BDD");
        assertNotNull(ticket.getPrice(), "Le prix est correctement injecter dana la BDD");
    }
    
    @Test
    public void testParkingLotExitRecurringUser() {
        // Arrange: Préparation pour le lancement du test
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Simuler les données pour un utilisateur récurrent
        String vehicleRegNumber = "ABCDEF";
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setParkingSpot(parkingSpot);

        // Simuler un inTime pour 1 heure avant maintenant
        Date inTime = new Date(0);
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure en arrière
        ticket.setInTime(inTime);

        // Ajouter un ticket existant pour simuler un utilisateur récurrent
        ticketDAO.saveTicket(ticket); // Premier ticket enregistré

        // Simuler la méthode processIncomingVehicle pour ajouter un nouveau ticket
        parkingService.processIncomingVehicle(inTime);

        // Act: Simuler la sortie du véhicule
        parkingService.processExitingVehicle(outTime);

        // Assert: Vérifier que la remise de 5% a été appliquée
        Ticket updatedTicket = ticketDAO.getTicket(vehicleRegNumber);
        assertNotNull(updatedTicket);

        // Calculer le prix attendu avec une remise de 5%
        double expectedPrice = (1 * Fare.CAR_RATE_PER_HOUR * 0.95); // 1 heure avec 5% de réduction
        assertEquals(expectedPrice, updatedTicket.getPrice(), 0.01); // Comparer le prix avec une tolérance de 0.01

    }

}
