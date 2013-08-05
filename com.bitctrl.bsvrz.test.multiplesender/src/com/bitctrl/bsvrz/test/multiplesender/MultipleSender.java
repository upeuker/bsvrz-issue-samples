package com.bitctrl.bsvrz.test.multiplesender;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.OneSubscriptionPerSendData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.application.StandardApplication;
import de.bsvrz.sys.funclib.application.StandardApplicationRunner;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;

public class MultipleSender implements StandardApplication {

	public class Sender extends Thread implements ClientSenderInterface {

		private int remaining;
		private final int intervalSec;
		private final ClientDavInterface dav;
		private byte sendeFreigabe = ClientSenderInterface.STOP_SENDING;

		private final SystemObject object;
		private final DataDescription dataDesc;

		public Sender(final ClientDavInterface dav, final String string,
				final int count, final int intervalSec) {

			this.dav = dav;
			this.remaining = count;
			this.intervalSec = intervalSec;

			final DataModel model = dav.getDataModel();
			object = model.getObject("fs.MQ.A81.5301.Ein.N.HFS");
			dataDesc = new DataDescription(
					model.getAttributeGroup("atg.verkehrsLageVerfahren1"),
					model.getAspect("asp.parameterVorgabe"));
			try {
				dav.subscribeSender(this, object, dataDesc, SenderRole.sender());
			} catch (final OneSubscriptionPerSendData e) {
				System.err.println(getName() + ": " + e.getLocalizedMessage()
						+ " --- ignorieren und trotzdem senden!");
			}
		}

		@Override
		public void run() {
			while (remaining > 0) {
				if (sendeFreigabe != ClientSenderInterface.START_SENDING) {
					System.err.println(getName() + " Noch keine Sendefreigabe");
					remaining++;
				} else {
					System.err.println(getName() + " schreibe Wert");
				}
				try {
					Thread.sleep(intervalSec * 1000);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
				remaining--;
			}

			System.err.println(getName() + " : Thread beendet sich");
			dav.unsubscribeSender(this, object, dataDesc);
		}

		@Override
		public void dataRequest(final SystemObject object,
				final DataDescription dataDesc, final byte status) {
			System.err.println(getName() + ": DataRequest: " + object + " --> "
					+ dataDesc + " Status == " + status);
			sendeFreigabe = status;
		}

		@Override
		public boolean isRequestSupported(final SystemObject object,
				final DataDescription dataDesc) {
			System.err.println(getName() + ": IsRequestSupported: " + object
					+ " --> " + dataDesc);
			return true;
		}
	}

	public static void main(final String[] args) {
		StandardApplicationRunner.run(new MultipleSender(), args);
	}

	@Override
	public void initialize(final ClientDavInterface dav) throws Exception {
		final Sender sender1 = new Sender(dav, "Sender1", 20, 2);
		final Sender sender2 = new Sender(dav, "Sender2", 30, 3);

		sender1.start();
		sender2.start();

		sender1.join();
		sender2.join();

		dav.disconnect(false, "Test beendet");
		System.exit(0);
	}

	@Override
	public void parseArguments(final ArgumentList argList) throws Exception {
	}
}
