package com.doliuw.service;

import com.doliuw.entity.Booking;
import com.doliuw.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email}")
    private String adminEmail;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─── Send booking confirmation to user + admin ────────────────

    @Async
    public void sendBookingConfirmation(User user, Booking booking) {
        sendUserBookingEmail(user, booking);
        sendAdminBookingEmail(user, booking);
    }

    // ─── User email ───────────────────────────────────────────────

    private void sendUserBookingEmail(User user, Booking booking) {
        String to = user.getEmail();
        if (to == null || to.isBlank()) {
            log.warn("User {} has no email – skipping user booking confirmation", user.getId());
            return;
        }

        String subject = "✅ Booking Confirmed – " + booking.getServiceName();

        String body = buildUserEmailHtml(user, booking);

        try {
            send(to, subject, body);
            log.info("Booking confirmation sent to user {} ({})", user.getId(), to);
        } catch (Exception e) {
            log.error("Failed to send booking email to user {}: {}", to, e.getMessage());
        }
    }

    // ─── Admin email ──────────────────────────────────────────────

    private void sendAdminBookingEmail(User user, Booking booking) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("Admin email not configured – skipping admin booking notification");
            return;
        }

        String subject = "📅 New Real Interview Booking – " + booking.getServiceName();
        String body = buildAdminEmailHtml(user, booking);

        try {
            send(adminEmail, subject, body);
            log.info("Admin booking notification sent to {}", adminEmail);
        } catch (Exception e) {
            log.error("Failed to send admin booking email: {}", e.getMessage());
        }
    }

    // ─── Core send ────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
    }

    // ─── HTML templates ───────────────────────────────────────────

    private String buildUserEmailHtml(User user, Booking booking) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1);">
                <div style="background:#000;padding:24px 32px;">
                  <h1 style="color:#fff;margin:0;font-size:22px;">Doliuw – Booking Confirmed</h1>
                </div>
                <div style="padding:32px;">
                  <p style="font-size:16px;">Hi <strong>%s</strong>,</p>
                  <p style="color:#555;">Your real interview booking has been confirmed. Here are your details:</p>

                  <table style="width:100%%;border-collapse:collapse;margin:24px 0;">
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;width:40%%;">Service</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">Booking ID</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">#%d</td>
                    </tr>
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">Date</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">Time Slot</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;">Amount Paid</td>
                      <td style="padding:12px 16px;font-weight:bold;color:#16a34a;">₹%d</td>
                    </tr>
                  </table>

                  <p style="color:#555;font-size:14px;">Please be available 5 minutes before your slot. Our team will reach out with any joining details.</p>
                  <p style="color:#555;font-size:14px;">If you need to reschedule or have questions, contact us from the Help section in your dashboard.</p>

                  <div style="margin-top:32px;padding-top:20px;border-top:1px solid #eee;color:#999;font-size:12px;">
                    <p>© Doliuw – From Campus to Career</p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                user.getName(),
                booking.getServiceName(),
                booking.getId(),
                booking.getBookingDate().format(DATE_FMT),
                booking.getTimeSlot(),
                booking.getPrice()
            );
    }

    private String buildAdminEmailHtml(User user, Booking booking) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;">
              <div style="max-width:600px;margin:auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1);">
                <div style="background:#1d4ed8;padding:24px 32px;">
                  <h1 style="color:#fff;margin:0;font-size:22px;">New Real Interview Booking</h1>
                </div>
                <div style="padding:32px;">
                  <p style="color:#555;">A new booking has been made. Details below:</p>

                  <table style="width:100%%;border-collapse:collapse;margin:24px 0;">
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;width:40%%;">Booking ID</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">#%d</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">Service</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">User Name</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">User Email</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">User Mobile</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">Date</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr style="background:#f9f9f9;">
                      <td style="padding:12px 16px;font-weight:bold;border-bottom:1px solid #eee;">Time Slot</td>
                      <td style="padding:12px 16px;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:12px 16px;font-weight:bold;">Amount</td>
                      <td style="padding:12px 16px;font-weight:bold;color:#16a34a;">₹%d</td>
                    </tr>
                  </table>

                  <div style="margin-top:32px;padding-top:20px;border-top:1px solid #eee;color:#999;font-size:12px;">
                    <p>© Doliuw Admin Notifications</p>
                  </div>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                booking.getId(),
                booking.getServiceName(),
                user.getName(),
                user.getEmail() != null ? user.getEmail() : "N/A",
                user.getMobile() != null ? user.getMobile() : "N/A",
                booking.getBookingDate().format(DATE_FMT),
                booking.getTimeSlot(),
                booking.getPrice()
            );
    }
}
