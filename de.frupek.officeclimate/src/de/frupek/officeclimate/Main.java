package de.frupek.officeclimate;

import java.text.DecimalFormat;

import com.tinkerforge.BrickletAmbientLight;
import com.tinkerforge.BrickletAmbientLight.IlluminanceListener;
import com.tinkerforge.BrickletDualButton;
import com.tinkerforge.BrickletDualButton.StateChangedListener;
import com.tinkerforge.BrickletHumidity;
import com.tinkerforge.BrickletHumidity.HumidityListener;
import com.tinkerforge.BrickletLCD20x4;
import com.tinkerforge.BrickletTemperature;
import com.tinkerforge.BrickletTemperature.TemperatureListener;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;

public class Main {
	private static final String HOST = "localhost";
	private static final int PORT = 4223;
	private static final String UID_LCD = "ocV";
	private static final String UID_TEMP = "q9U";
	private static final String UID_HUM = "nxe";
	private static final String UID_BUT = "mrz";
	private static final String UID_AMB = "mfx";
	private static boolean isCels = true;
	private static final short degSignIndex = 0; 
	
	public static void main(String args[]) throws Exception {
		IPConnection ipcon = new IPConnection(); 
		final BrickletLCD20x4 lcd = new BrickletLCD20x4(UID_LCD, ipcon); 
		BrickletHumidity hum = new BrickletHumidity(UID_HUM, ipcon);
		BrickletTemperature temp = new BrickletTemperature(UID_TEMP, ipcon);
		BrickletAmbientLight amb = new BrickletAmbientLight(UID_AMB, ipcon);
		BrickletDualButton but = new BrickletDualButton(UID_BUT, ipcon);
		
		ipcon.connect(HOST, PORT); 

		lcd.backlightOn();
		lcd.clearDisplay();
		
		Main.addDegSign(lcd, degSignIndex);
		
		Main.updateTemperature(lcd, temp);
		Main.writeLine(lcd, 1, Main.getHumidityString(hum.getHumidity()));
		Main.writeLine(lcd, 2, Main.getAmbientLightString(amb.getIlluminance()));
		Main.writeLine(lcd, 3, "(C)'15 by Frupek");
		
		temp.setTemperatureCallbackPeriod(1000);
		hum.setHumidityCallbackPeriod(1000);
		amb.setIlluminanceCallbackPeriod(1000);
		
		Main.toggleToCelsius(but);
		
		temp.addTemperatureListener(new TemperatureListener() {
			@Override
			public void temperature(short temperature) {
				String str = Main.getTemperatureString(temperature, isCels);
				Main.writeLine(lcd, 0, str);
			}
		});
		
		hum.addHumidityListener(new HumidityListener() {
			@Override
			public void humidity(int humidity) {
				Main.writeLine(lcd, 1, Main.getHumidityString(humidity));
			}
		});

		amb.addIlluminanceListener(new IlluminanceListener() {
			@Override
			public void illuminance(int illuminance) {
				Main.writeLine(lcd, 2, Main.getAmbientLightString(illuminance));
			}
		});
		
		but.addStateChangedListener(new StateChangedListener() {
			@Override
			public void stateChanged(short buttonL, short buttonR, short ledL, short ledR) {
				if (buttonL == BrickletDualButton.BUTTON_STATE_PRESSED) {
					Main.toggleToCelsius(but);
					isCels = true;
				}
				else if (buttonR == BrickletDualButton.BUTTON_STATE_PRESSED) {
					Main.toggleToFahrenheit(but);
					isCels = false;
				}
				Main.updateTemperature(lcd, temp);
			}
		});
	
		
		System.out.println("Press key to exit");
		System.in.read();
		
		Main.writeLine(lcd, 0, "                    ");
		Main.writeLine(lcd, 1, "                    ");
		Main.writeLine(lcd, 2, "                    ");
		
		ipcon.disconnect();
	}
	
	/**
	 * Schaltet auf Celsius um
	 * @param but Button Bricklet
	 */
	private static void toggleToCelsius(BrickletDualButton but) {
		if (but == null) {
			throw new IllegalArgumentException("Instance of BrickletDualButton required");
		}
		
		try {
			but.setLEDState(BrickletDualButton.LED_STATE_ON, BrickletDualButton.LED_STATE_OFF);
		} catch (TimeoutException | NotConnectedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Schaltet auf Fahrenheit um
	 * @param but Button Bricklet
	 */
	private static void toggleToFahrenheit(BrickletDualButton but) {
		if (but == null) {
			throw new IllegalArgumentException("Instance of BrickletDualButton required");
		}
		
		try {
			but.setLEDState(BrickletDualButton.LED_STATE_OFF, BrickletDualButton.LED_STATE_ON);
		} catch (TimeoutException | NotConnectedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Liefert die Temperaturanzeige aus den gelesenen Daten
	 * @param temperature Temperatur vom Bricklet gelesen
	 * @param cels true für Anzeige in Grad Celsius, false für Fahrenheit
	 * @return Ausgabestring
	 */
	private static String getTemperatureString(short temperature, boolean cels) {
		double temp = (temperature/100.0);
		String unit = "C";
		
		if (cels == false) {
			temp = temp * 1.8 + 32;
			unit = "F";
		}
		
		DecimalFormat df = new DecimalFormat("#.0");
		
		return "Temp.:     " + df.format(temp) + " " + (char)Main.getBytePosToIndex(degSignIndex) + unit;
	}
	
	/**
	 * Aktualisiert die Temperaturzeile
	 * @param lcd LCD Bricklet
	 * @param temp Temperature Bricklet
	 */
	private static void updateTemperature(BrickletLCD20x4 lcd, BrickletTemperature temp) {
		try {
			Main.writeLine(lcd, 0, Main.getTemperatureString(temp.getTemperature(), isCels));
		} catch (TimeoutException | NotConnectedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Liefert die Anzeige der relativen Luftfeuchtigkeit aus den gelesenen
	 * Daten.
	 * @param humidity Gelesene Luftfeuchtigkeit vom Bricklet
	 * @return Ausgabestring
	 */
	private static String getHumidityString(int humidity) {
		return "Rel. Hum.: " + (humidity/10.0) + " %";
	}
	
	/**
	 * Liefert die Anzeige der Beleuchtungsstärke
	 * @param illum Beleuchtungsstärke
	 * @return Ausgabestring
	 */
	private static String getAmbientLightString(int illum) {
		return "Illum.:    " + (illum/10.0) + " lx";
	}
	
	/**
	 * Schreibt eine Zeile im LCD Display
	 * @param lcd Instanz des LCD Bricklets
	 * @param lineIndex Zeilenindex, nullbasiert
	 * @param string Zu schreibender String
	 */
	private static void writeLine(BrickletLCD20x4 lcd, int lineIndex, String string) {
		if (lcd == null) {
			throw new IllegalArgumentException("Instance of BrickletLCD20x4 required");
		}
		try {
			lcd.writeLine((short)lineIndex, (short)0, "                      ");
			lcd.writeLine((short)lineIndex, (short)0, string);
		} catch (TimeoutException | NotConnectedException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Liefert die Byteposition zur Verwendung eines benutzerdefinierten 
	 * Zeichens 
	 * @param index Index des Zeichens
	 * @return Byteposition
	 */
	private static byte getBytePosToIndex(short index) {
		byte result = 0x08;
		
		return (byte) (result + (byte)index);
	}
	
	/**
	 * Setzt das Gradsymbol zum gewünschten Index
	 * @param lcd LCD Bricklet
	 * @param index Index des Symbols
	 */
	private static void addDegSign(BrickletLCD20x4 lcd, short index) {
		if (lcd == null) {
			throw new IllegalArgumentException("Instance of BrickletLCD20x4 required");
		}
		
		short[] character = new short[8]; 
	    character[0] = 0b00000110;
	    character[1] = 0b00001001;
	    character[2] = 0b00001001;
	    character[3] = 0b00000110;
	    character[4] = 0b00000000;
	    character[5] = 0b00000000;
	    character[6] = 0b00000000;
	    character[7] = 0b00000000;

	    try {
			lcd.setCustomCharacter(index, character);
		} catch (TimeoutException | NotConnectedException e) {
			e.printStackTrace();
		}
	}
}
