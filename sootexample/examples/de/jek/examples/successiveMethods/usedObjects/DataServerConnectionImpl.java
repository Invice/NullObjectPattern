package de.jek.examples.successiveMethods.usedObjects;

import java.util.logging.Logger;

import de.jek.examples.presentation.usedObjects.Customer;
import de.jek.examples.presentation.usedObjects.Stock;

public class DataServerConnectionImpl implements IDataServerConnection {

	private static Logger LOGGER = Logger.getLogger("DataServerConnection");

	private Stock stock = new Stock();

	/**
	 * Dummy implementation which just spends time.
	 * 
	 * @throws Exception
	 */
	@Override
	public boolean connect() throws ConnectionException {

		LOGGER.info("Trying to connect to DataServer...");
		workWithTheDatabase(8000);
		LOGGER.info("Connection to DataServer successfully established...");
		return true;
	}

	@Override
	public String connect(int timeout) throws ConnectionException {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < timeout) {
			if (connect()) {
				return "hostname";
			}
		}
		return null;
	}

	@Override
	public void updateProducts(IEventServerConnection eventSC)
			throws ConnectionException {
		LOGGER.info("Trying to update products...");
		workWithTheDatabase(8000);
		LOGGER.info("Products updated...");
	}

	@Override
	public void updateProductPrices(IEventServerConnection eventSC)
			throws ConnectionException {
		LOGGER.info("Trying to connect to DataServer...");
		workWithTheDatabase(7000);
		LOGGER.info("Connection to DataServer successfully established...");
	}

	@Override
	public void updateAvailability(IEventServerConnection eventSC)
			throws ConnectionException {
		LOGGER.info("update availability in stock...");
		workWithTheDatabase(8000);
		stock.setAvailabilityFor(7777, 66);
		LOGGER.info("availability updated...");
	}

	@Override
	public int addNewCustomers(IEventServerConnection eventSC)
			throws ConnectionException {
		LOGGER.info("add new customers...");
		workWithTheDatabase(4000);
		LOGGER.info("new customers added...");
		return (int) (Math.random() * 1000);
	}

	private static void workWithTheDatabase(int ms) throws ConnectionException {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public Customer retrieveCustomer(long customerid)
			throws ConnectionException {
		LOGGER.info("retrieve customer with id " + customerid);
		workWithTheDatabase(4000);
		Customer customer = new Customer(customerid, dummyName(),
				dummyNoPurchases(), dummyPostcode());
		return customer;
	}

	private String dummyPostcode() {
		String[] postcodes = { "24340", "PO04 BZ", "12345", "33333", "24114" };
		int i = (int) Math.random() * 4;
		return postcodes[i];
	}

	private int dummyNoPurchases() {
		int[] noPurchases = { 0, 0, 1, 1, 2, 3, 4 };
		int i = (int) Math.random() * 6;
		return noPurchases[i];
	}

	private String dummyName() {
		String[] names = { "Smith", "Potter", "Black", "Peterson", "Jackson" };
		int i = (int) Math.random() * 4;
		return names[i];
	}

	@Override
	public void incrementNoPurchasesOfCustomer(long id)
			throws ConnectionException {
		LOGGER.info("increment no purchases of customer with id" + id);
		workWithTheDatabase(4000);
		LOGGER.info("customer updated...");

	}

	@Override
	public void updatePostcodeNoPurchasesRelation(String postcode,
			int noPurchases) throws ConnectionException {
		LOGGER.info("update relation postcode: " + postcode
				+ " number purchases : " + noPurchases);
		workWithTheDatabase(4000);
		LOGGER.info("new relation added...");
	}

	@Override
	public void addPostcodeProductIDRelation(String postcode, int productid)
			throws ConnectionException {
		LOGGER.info("add relation postcode: " + postcode + " product id : "
				+ productid);
		workWithTheDatabase(4000);
		LOGGER.info("new relation added...");
	}

}
