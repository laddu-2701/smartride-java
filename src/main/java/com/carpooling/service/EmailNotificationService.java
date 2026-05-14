package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    public boolean sendRegistrationEmail(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }

        String subject = "Welcome to Carpooling";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Your account has been registered successfully as " + user.getRole() + ".\n"
                + "You can now login and start using Carpooling.\n\n"
                + "Thanks,\nCarpooling Team";

        return sendEmail(user.getEmail(), subject, body);
    }

    public boolean sendBookingConfirmationEmail(User passenger, Booking booking) {
        if (passenger == null || booking == null || passenger.getEmail() == null || passenger.getEmail().isBlank()) {
            return false;
        }

        String subject = "Ride Booking Confirmed";
        String body = "Hi " + passenger.getName() + ",\n\n"
                + "Your ride booking is confirmed.\n"
                + "Booking details:\n"
                + "- Ride ID: " + booking.getRideId() + "\n"
                + "- Seats Booked: " + booking.getSeatsBooked() + "\n"
                + "- Total Price: $" + booking.getTotalPrice() + "\n\n"
                + "Thanks for using Smart Ride.\n\n"
                + "Regards,\nCarpooling Team";

        return sendEmail(passenger.getEmail(), subject, body);
    }

    public boolean sendRideCancelledEmail(User passenger, Booking booking) {
        if (passenger == null || passenger.getEmail() == null || passenger.getEmail().isBlank()) {
            return false;
        }
        String subject = "Your Ride Has Been Cancelled";
        String body = "Hi " + passenger.getName() + ",\n\n"
                + "We're sorry to inform you that the ride you booked has been CANCELLED by the driver.\n"
                + "Booking details:\n"
                + "- Ride ID: " + booking.getRideId() + "\n"
                + "- Seats Booked: " + booking.getSeatsBooked() + "\n"
                + "- From: " + booking.getPassengerSource() + "\n"
                + "- To: " + booking.getPassengerDestination() + "\n\n"
                + "A refund (if applicable) will be processed shortly.\n\n"
                + "We apologise for the inconvenience.\n\nRegards,\nCarpooling Team";
        return sendEmail(passenger.getEmail(), subject, body);
    }

    public boolean sendRideRescheduledEmail(User passenger, Booking booking, String newDate, String newTime) {
        if (passenger == null || passenger.getEmail() == null || passenger.getEmail().isBlank()) {
            return false;
        }
        String subject = "Your Ride Has Been Rescheduled";
        String body = "Hi " + passenger.getName() + ",\n\n"
                + "Your upcoming ride has been RESCHEDULED by the driver.\n"
                + "Updated details:\n"
                + "- Ride ID: " + booking.getRideId() + "\n"
                + "- New Date: " + newDate + "\n"
                + "- New Time: " + newTime + "\n"
                + "- From: " + booking.getPassengerSource() + "\n"
                + "- To: " + booking.getPassengerDestination() + "\n\n"
                + "Please check your dashboard for the latest details.\n\nRegards,\nCarpooling Team";
        return sendEmail(passenger.getEmail(), subject, body);
    }

    public boolean sendRideReminderEmail(User passenger, Booking booking, String rideDate, String rideTime) {
        if (passenger == null || passenger.getEmail() == null || passenger.getEmail().isBlank()) {
            return false;
        }
        String subject = "Upcoming Ride Reminder";
        String body = "Hi " + passenger.getName() + ",\n\n"
                + "This is a reminder for your upcoming ride.\n"
                + "Ride details:\n"
                + "- Ride ID: " + booking.getRideId() + "\n"
                + "- Date: " + rideDate + "\n"
                + "- Time: " + rideTime + "\n"
                + "- From: " + booking.getPassengerSource() + "\n"
                + "- To: " + booking.getPassengerDestination() + "\n\n"
                + "Please be ready a few minutes before departure.\n\n"
                + "Regards,\nCarpooling Team";
        return sendEmail(passenger.getEmail(), subject, body);
    }

    public boolean sendLoginNotificationEmail(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        String subject = "Login Alert - Smart Ride";
        String body = "Hi " + user.getName() + ",\n\n"
                + "Your Smart Ride account was just used to log in.\n"
                + "If this was you, no further action is needed. If you did not log in, please secure your account immediately.\n\n"
                + "Regards,\nCarpooling Team";
        return sendEmail(user.getEmail(), subject, body);
    }

    private boolean sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            // Email failure should not block API success paths.
            logger.warn("Email send failed to {}: {}", to, ex.getMessage());
            return false;
        }
    }
}
