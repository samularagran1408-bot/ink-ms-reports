package com.inklusport.reports.service;

import com.inklusport.reports.dto.WeeklyScheduleResponse;
import com.inklusport.reports.entity.ReportConfig;
import com.inklusport.reports.repository.ReportConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyReportScheduleServiceTest {

    @Mock
    private ReportConfigRepository reportConfigRepository;
    @Mock
    private PdfExportService pdfExportService;
    @Mock
    private ReportEmailService reportEmailService;

    private WeeklyReportScheduleService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyReportScheduleService(
                reportConfigRepository, pdfExportService, reportEmailService);
        when(reportConfigRepository.save(any(ReportConfig.class)))
                .thenAnswer(invocation -> {
                    ReportConfig config = invocation.getArgument(0);
                    if (config.getId() == null) {
                        config.setId("cfg-1");
                    }
                    return config;
                });
    }

    @Test
    void cp7Hu35_programaEnvioSemanal() {
        when(reportConfigRepository.findFirstByOwnerIdAndScheduleFrequency(
                "admin@inklusport.test", WeeklyReportScheduleService.FREQUENCY_WEEKLY))
                .thenReturn(Optional.empty());

        WeeklyScheduleResponse response = service.scheduleWeekly("Admin@Inklusport.Test");

        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getRecipientEmail()).isEqualTo("admin@inklusport.test");
        assertThat(response.getFrequency()).isEqualTo("WEEKLY");

        ArgumentCaptor<ReportConfig> captor = ArgumentCaptor.forClass(ReportConfig.class);
        verify(reportConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getScheduleEnabled()).isTrue();
        assertThat(captor.getValue().getScheduleFrequency()).isEqualTo("WEEKLY");
    }

    @Test
    void cp8Hu35_cancelaProgramacionActiva() {
        ReportConfig existing = ReportConfig.builder()
                .id("cfg-1")
                .ownerId("admin@inklusport.test")
                .reportName(WeeklyReportScheduleService.WEEKLY_REPORT_NAME)
                .filters("{}")
                .scheduleEnabled(true)
                .scheduleFrequency(WeeklyReportScheduleService.FREQUENCY_WEEKLY)
                .recipientEmail("admin@inklusport.test")
                .build();
        when(reportConfigRepository.findFirstByOwnerIdAndScheduleFrequency(
                "admin@inklusport.test", WeeklyReportScheduleService.FREQUENCY_WEEKLY))
                .thenReturn(Optional.of(existing));

        WeeklyScheduleResponse response = service.cancelWeekly("admin@inklusport.test");

        assertThat(response.isEnabled()).isFalse();
        assertThat(existing.getScheduleEnabled()).isFalse();
        verify(reportConfigRepository).save(existing);
    }

    @Test
    void processDueWeeklyReports_enviaPdf() {
        ReportConfig due = ReportConfig.builder()
                .id("cfg-1")
                .ownerId("admin@inklusport.test")
                .recipientEmail("admin@inklusport.test")
                .scheduleEnabled(true)
                .scheduleFrequency(WeeklyReportScheduleService.FREQUENCY_WEEKLY)
                .filters("{}")
                .reportName(WeeklyReportScheduleService.WEEKLY_REPORT_NAME)
                .build();
        when(reportConfigRepository.findByScheduleEnabledTrueAndScheduleFrequency("WEEKLY"))
                .thenReturn(List.of(due));
        when(pdfExportService.exportDashboard(any())).thenReturn(new byte[]{1, 2, 3});
        when(reportEmailService.sendWeeklyReport(any(), any(), any(), any(), any())).thenReturn(true);

        int sent = service.processDueWeeklyReports();

        assertThat(sent).isEqualTo(1);
        verify(reportEmailService).sendWeeklyReport(
                any(), any(), any(), any(), any());
    }
}
