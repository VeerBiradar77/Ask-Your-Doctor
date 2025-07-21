package com.app.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import com.app.controller.PatientController;
import com.app.custome_exception.UserHandlingException;
import com.app.entity.modal.Appointment;
import com.app.entity.modal.Doctor;
import com.app.entity.modal.Patient;
import com.app.repository.AppointmentRepository;
import com.app.service.intf.DoctorServiceIntf;
import com.app.service.intf.EmailSenderServiceIntf;
import com.app.service.intf.PatientServiceIntf;

@Service
@Transactional
public class EmailSenderService implements EmailSenderServiceIntf {

    private final PatientController patientController;

    @Autowired
    private PatientServiceIntf patientService;

    @Autowired
    private DoctorServiceIntf doctorService;

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private JavaMailSender mailSender;

    public EmailSenderService(PatientController patientController) {
        this.patientController = patientController;
    }

    public void sendSimpleEmail(String toEmail, String body, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("ask.your.doctor.springboot.app@gmail.com");
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);

        mailSender.send(message);
        System.out.println("Simple message sent....");
    }

    @Override
    public void sendEmailOnAppointmentBooking(Long patientId, Long doctorId, String time) {
        try {
            Patient patient = patientService.getPatientDetails(patientId);
            Doctor doctor = doctorService.getDoctorDetails(doctorId);

            byte[] pdfBytes = generateAppointmentPdf(patient, doctor, time);

            sendEmailWithAttachment(
                    patient.getEmail(),
                    "Your appointment has been booked at " + time + ". Please find the details attached.",
                    "Appointment Confirmation",
                    pdfBytes
            );
        } catch (IOException | MessagingException e) {
            System.err.println("Failed to send appointment confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] generateAppointmentPdf(Patient patient, Doctor doctor, String time) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                contentStream.setLeading(20f);
                contentStream.newLineAtOffset(50, 700);

                contentStream.showText("Appointment Confirmation!!!");
                contentStream.newLine();
                contentStream.newLine();

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.showText("Patient name: " + patient.getFirstName() + " " + patient.getLastName());
                contentStream.newLine();
                contentStream.showText("Patient email: " + patient.getEmail());
                contentStream.newLine();
                contentStream.showText("Doctor name: " + doctor.getFirstName() + " " + doctor.getLastName());
                contentStream.newLine();
                contentStream.showText("Appointment time : " + time);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private void sendEmailWithAttachment(String toMail, String body, String subject, byte[] pdfBytes) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        helper.setFrom("ask.your.doctor.springboot.app@gmail.com");
        helper.setTo(toMail);
        helper.setSubject(subject);
        helper.setText(body);

        helper.addAttachment("AppointmentDetails.pdf", new ByteArrayResource(pdfBytes));
        mailSender.send(mimeMessage);
        System.out.println("Email with PDF attachment sent...");
    }

	
	@Override
	public void sendEmailTokenToResetPassword(String userEmail, Long token) {
		sendSimpleEmail(userEmail, 
				"Token to reset your password : "+token,
				"Reset Password");
	}
	
	@Override
	public void sendEmailOnCancelAppointment(Long appointmentId) {
	
		Appointment appointment = appointmentRepo.findById(appointmentId).orElseThrow(() -> new UserHandlingException("Invalid Appointment id!!!"));
		Doctor doctor = appointment.getDoctor();
		Patient patient = appointment.getPatient();
		
		sendSimpleEmail(patient.getEmail(), 
				"Your appointment has been cancelled with doctor : "+doctor.getFirstName(),
				"Appointment Cancelled");
		
		sendSimpleEmail(doctor.getEmail(), 
				"Appointment with patient : "+patient.getFirstName()+" has been cancelled",
				"Appointment Cancelled");
	}
	
	
	
}
