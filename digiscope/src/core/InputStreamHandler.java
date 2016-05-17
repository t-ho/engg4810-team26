package core;

import java.io.IOException;

import org.jfree.data.xy.XYSeries;

import data.CommandPacket;
import data.Constant;
import data.DataPacket;
import data.Packet;
import data.PacketFormatException;
import data.PacketType;

/**
 *
 * @author ToanHo
 */
public class InputStreamHandler implements Runnable {

	private MainWindow mainWindow_;
	private PacketWriter packetWriter_;
	private PacketReader packetReader_;

	public InputStreamHandler(MainWindow mainWindow, PacketReader packetReader, PacketWriter packetWriter) {
		mainWindow_ = mainWindow;
		packetReader_ = packetReader;
		packetWriter_ = packetWriter;
	}

	@Override
	public void run() {
		// TODO
		try {
			boolean isComplete = false;
			int aCurrentTime = 0;
			int aTimeDistance = 1; // 1 us
			int bCurrentTime = 0;
			int bTimeDistance = 1;
			double aMaxDiplayVoltage = 0;
			double aMinDiplayVoltage = 0;
			double bMaxDiplayVoltage = 0;
			double bMinDiplayVoltage = 0;
			XYSeries aSeries = new XYSeries(Constant.CHANNEL_A);
			XYSeries bSeries = new XYSeries(Constant.CHANNEL_B);
			while (isComplete == false) {
				Packet packet = packetReader_.nextPacket();
				if (packet != null) {
					byte type = packet.getType();
					if (packet instanceof CommandPacket) {
						CommandPacket commandPacket = (CommandPacket) packet;
						byte indicator = commandPacket.getIndicator();
						if (indicator == Constant.CONFIRMATION) {
							switch (type) {
							case PacketType.VERTICAL_RANGE_A:
								mainWindow_.setVerticalRange(Constant.CHANNEL_A, commandPacket.getArgument());
								break;

							case PacketType.VERTICAL_RANGE_B:
								mainWindow_.setVerticalRange(Constant.CHANNEL_B, commandPacket.getArgument());
								break;

							case PacketType.HORIZONTAL_RANGE:
								mainWindow_.setHorizontalRange(commandPacket.getArgument());
								break;

							case PacketType.TRIGGER_MODE:
								mainWindow_.setTriggerMode(commandPacket.getArgument());
								break;

							case PacketType.TRIGGER_TYPE:
								mainWindow_.setTriggerType(commandPacket.getArgument());
								break;

							case PacketType.TRIGGER_THRESHOLD:
								//TODO
								break;

							case PacketType.CHANNEL_COUPLING:
								mainWindow_.setChannelCoupling(commandPacket.getArgument());
							}
						}
					} else if (packet instanceof DataPacket) {
						DataPacket dataPacket = (DataPacket) packet;
						short sequenceNumber = dataPacket.getSequenceNumber();
						short nSamples = dataPacket.getNumberOfSamples();
						short samples[] = dataPacket.getSamples();
						switch (type) {
						case PacketType.CHANNEL_A_8_BITS:
							if (sequenceNumber == 0) { // Start a new series for channel A
								aSeries = new XYSeries(Constant.CHANNEL_A);
								aCurrentTime = 0;
								aTimeDistance = 1; // 1 us
								aMaxDiplayVoltage = mainWindow_.getMaxDisplayVoltage(Constant.A_INDEX);
								aMinDiplayVoltage = mainWindow_.getMinDisplayVoltage(Constant.A_INDEX);
							}
							for (short i = 0; i < nSamples; i++) {
								double voltage = (aMaxDiplayVoltage - aMinDiplayVoltage) * ((double) samples[i] / 255.0)
										+ aMinDiplayVoltage;
								aSeries.add(aCurrentTime, voltage);
								aCurrentTime += aTimeDistance;
							}
							mainWindow_.setXYSeriesForChannel(Constant.CHANNEL_A, aSeries);

						case PacketType.CHANNEL_B_8_BITS:
							if (sequenceNumber == 0) { // Start a new series for channel B
								bSeries = new XYSeries(Constant.CHANNEL_B);
								bCurrentTime = 0;
								bTimeDistance = 1; // 1 us
								bMaxDiplayVoltage = mainWindow_.getMaxDisplayVoltage(Constant.B_INDEX);
								bMinDiplayVoltage = mainWindow_.getMinDisplayVoltage(Constant.B_INDEX);
							}
							for (short i = 0; i < nSamples; i++) {
								double voltage = (bMaxDiplayVoltage - bMinDiplayVoltage) * ((double) samples[i] / 255.0)
										+ bMinDiplayVoltage;
								bSeries.add(bCurrentTime, voltage);
								bCurrentTime += bTimeDistance;
							}
							mainWindow_.setXYSeriesForChannel(Constant.CHANNEL_B, bSeries);

						case PacketType.CHANNEL_A_12_BITS:
							if (sequenceNumber == 0) { // Start a new series for channel A
								aSeries = new XYSeries(Constant.CHANNEL_A);
								aCurrentTime = 0;
								aTimeDistance = 1; // 1 us
								aMaxDiplayVoltage = mainWindow_.getMaxDisplayVoltage(Constant.A_INDEX);
								aMinDiplayVoltage = mainWindow_.getMinDisplayVoltage(Constant.A_INDEX);
							}
							for (short i = 0; i < nSamples; i++) {
								double voltage = (aMaxDiplayVoltage - aMinDiplayVoltage) * ((double) samples[i] / 4095.0)
										+ aMinDiplayVoltage;
								aSeries.add(aCurrentTime, voltage);
								aCurrentTime += aTimeDistance;
							}
							mainWindow_.setXYSeriesForChannel(Constant.CHANNEL_A, aSeries);

						case PacketType.CHANNEL_B_12_BITS:
							if (sequenceNumber == 0) { // Start a new series for channel B
								bSeries = new XYSeries(Constant.CHANNEL_B);
								bCurrentTime = 0;
								bTimeDistance = 1; // 1 us
								bMaxDiplayVoltage = mainWindow_.getMaxDisplayVoltage(Constant.B_INDEX);
								bMinDiplayVoltage = mainWindow_.getMinDisplayVoltage(Constant.B_INDEX);
							}
							for (short i = 0; i < nSamples; i++) {
								double voltage = (bMaxDiplayVoltage - bMinDiplayVoltage) * ((double) samples[i] / 4095.0)
										+ bMinDiplayVoltage;
								bSeries.add(bCurrentTime, voltage);
								bCurrentTime += bTimeDistance;
							}
							mainWindow_.setXYSeriesForChannel(Constant.CHANNEL_B, bSeries);
						}
					} 
				}
			}
		} catch (PacketFormatException pfe) {
			pfe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
