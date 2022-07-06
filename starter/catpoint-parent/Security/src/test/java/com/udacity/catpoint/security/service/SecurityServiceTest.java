package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.image.service.ImageServiceInterface;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    public SecurityService securityService;

    @Mock
    public ImageServiceInterface imageService;

    @Mock
    public SecurityRepository securityRepository;

    @Mock
    public StatusListener statusListener;

    static HashSet<Sensor> sensors = new HashSet<>();
    Sensor sensor = new Sensor("DOOR" , SensorType.DOOR);

    static void sensorSet(){
        Sensor sensor_door = new Sensor("DOOR", SensorType.DOOR);
        Sensor sensor_motion = new Sensor("MOTION", SensorType.MOTION);
        Sensor sensor_window = new Sensor("WINDOW", SensorType.WINDOW);
        sensors.add((sensor_door));
        sensors.add((sensor_window));
        sensors.add((sensor_motion));

    }

    @BeforeEach
    void setUp(){
        securityService = new SecurityService(securityRepository , imageService);
    }

    @Test
    public void addStatusListener(){
        securityService.addStatusListener(statusListener);
    }

    @Test
    public void removeStatusListener(){
        securityService.addStatusListener(statusListener);
    }

    @Test
    public void addSensor(){
        securityService.addSensor(sensor);
    }

    @Test
    public void removeSensor(){
        securityService.removeSensor(sensor);
    }

    @Test   // 1
    public void shouldBe_In_PendingAlarmStatus ()
    {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test   // 2
    public void shouldSet_Pending_AlarmStatus_To_Armed () {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test   // 3
    public void shouldReturn_NoAlarmStatus () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test   // 4
    public void shouldNot_Change_AlarmState () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test   //5
    public void sensor_ActiveSystem_Pending_ChangeToAlarm () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test   // 6
    public void sensorDeactivated_WhileInactive_NoChangeToAlarmState () {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test   // 7
    public void catIdentified_While_ArmedHomeChangeSystem_To_Alarm () {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test   // 8
    public void catNotIdentifiedChange_Status_NoAlarm_IfSensorsInactive () {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        sensor.setActive(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test   // 9
    public void system_DisArmed_SetStatus_NoAlarm () {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest  // 10
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    public void systemArmed_ResetSensors_ToInactive (ArmingStatus status) {
        sensorSet();
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        sensors.forEach(sensor -> sensor.setActive(true));
        securityService.setArmingStatus(status);
        sensors.forEach(sensor -> Assertions.assertEquals(false, sensor.getActive()));
    }


    @Test   // 11
    public void systemArmedHome_CatIdentified_SetStatusAlarm () {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    @Test   // 12
    public void system_Armed_SetStatusAlarm () {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(securityService.getArmingStatus());
        verify(securityRepository, times(2)).setAlarmStatus(AlarmStatus.ALARM);
    }




}