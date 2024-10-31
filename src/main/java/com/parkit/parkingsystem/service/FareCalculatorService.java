package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) 
    {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount){

        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        double duration = outHour - inHour;
        double durationHours = duration / (1000 * 60 * 60 );

        if (discount) {
            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice(durationHours * (Fare.CAR_RATE_PER_HOUR * 0.95));
                    break;
                }
                case BIKE: {
                    ticket.setPrice(durationHours * (Fare.BIKE_RATE_PER_HOUR * 0.95));
                    break;
                }
                default: throw new IllegalArgumentException("Unkown Parking Type");
            }
        } else {
            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default: throw new IllegalArgumentException("Unkown Parking Type");
            }            
        }

       
        // implementation des 30 minutes gratuites
        if( durationHours <= 0.5) {
            ticket.setPrice(0);
            return;
        } 

           
    }
}