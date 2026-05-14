package com.carpooling.service;

import com.carpooling.model.Booking;
import com.carpooling.model.Notification;
import com.carpooling.model.Ride;
import com.carpooling.model.User;
import com.carpooling.repository.BookingRepository;
import com.carpooling.repository.NotificationRepository;
import com.carpooling.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideService rideService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private SmsNotificationService smsNotificationService;

    @Value("${ride.reminder.hoursBefore:2}")
    private int reminderHoursBefore;

    public Notification createInAppNotification(Long userId,
                                                String type,
                                                String title,
                                                String message,
                                                String referenceType,
                                                Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public Notification markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("Not allowed to update this notification");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void notifyBookingConfirmation(User passenger, Booking booking) {
        String title = "Booking confirmed";
        String message = "Your booking #" + booking.getId() + " is confirmed for ride #" + booking.getRideId() + ".";
        createInAppNotification(passenger.getId(), "BOOKING_CONFIRMATION", title, message, "BOOKING", booking.getId());
        emailNotificationService.sendBookingConfirmationEmail(passenger, booking);
        smsNotificationService.sendSms(passenger.getPhone(), "SmartRide: " + message);
    }

    public void notifyRideCancelled(User passenger, Booking booking) {
        String title = "Ride cancelled";
        String message = "Ride #" + booking.getRideId() + " has been cancelled by the driver.";
        createInAppNotification(passenger.getId(), "RIDE_UPDATE", title, message, "RIDE", booking.getRideId());
        emailNotificationService.sendRideCancelledEmail(passenger, booking);
        smsNotificationService.sendSms(passenger.getPhone(), "SmartRide: " + message);
    }

    public void notifyRideRescheduled(User passenger, Booking booking, String newDate, String newTime) {
        String title = "Ride rescheduled";
        String message = "Ride #" + booking.getRideId() + " moved to " + newDate + " " + newTime + ".";
        createInAppNotification(passenger.getId(), "RIDE_UPDATE", title, message, "RIDE", booking.getRideId());
        emailNotificationService.sendRideRescheduledEmail(passenger, booking, newDate, newTime);
        smsNotificationService.sendSms(passenger.getPhone(), "SmartRide: " + message);
    }

    @Scheduled(fixedDelayString = "${ride.reminder.scanMs:300000}")
    public void sendRideReminders() {
        List<Booking> confirmedBookings = bookingRepository.findByBookingStatusIgnoreCase("CONFIRMED");
        LocalDateTime now = LocalDateTime.now();

        for (Booking booking : confirmedBookings) {
            if (booking.getReminderSentAt() != null) {
                continue;
            }
            Ride ride = rideService.findById(booking.getRideId());
            if (ride == null || !"SCHEDULED".equalsIgnoreCase(ride.getStatus()) || ride.getDate() == null || ride.getTime() == null) {
                continue;
            }

            LocalDateTime rideDateTime = LocalDateTime.of(ride.getDate(), ride.getTime());
            LocalDateTime triggerTime = rideDateTime.minusHours(Math.max(reminderHoursBefore, 1));
            if (now.isBefore(triggerTime) || now.isAfter(rideDateTime)) {
                continue;
            }

            User passenger = userRepository.findById(booking.getPassengerId()).orElse(null);
            if (passenger == null) {
                continue;
            }

            String title = "Ride reminder";
            String message = "Reminder: ride #" + ride.getId() + " starts at " + ride.getDate() + " " + ride.getTime() + ".";
            boolean duplicate = notificationRepository.existsByUserIdAndTypeAndReferenceTypeAndReferenceId(
                    passenger.getId(), "RIDE_REMINDER", "BOOKING", booking.getId());
            if (!duplicate) {
                createInAppNotification(passenger.getId(), "RIDE_REMINDER", title, message, "BOOKING", booking.getId());
                emailNotificationService.sendRideReminderEmail(
                        passenger,
                        booking,
                        ride.getDate().toString(),
                        ride.getTime().toString()
                );
                smsNotificationService.sendSms(passenger.getPhone(), "SmartRide: " + message);
            }

            booking.setReminderSentAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
    }
}
