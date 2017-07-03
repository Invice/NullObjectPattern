package de.jek.examples.successiveMethods.usedObjects;

import de.jek.examples.presentation.usedObjects.Customer;

public interface IDataServerConnection {

	public boolean connect() throws ConnectionException;

	public String connect(int timeout) throws ConnectionException;

	public void updateProducts(IEventServerConnection eventSC)
			throws ConnectionException;

	public void updateProductPrices(IEventServerConnection eventSC)
			throws ConnectionException;

	public void updateAvailability(IEventServerConnection eventSC)
			throws ConnectionException;

	public int addNewCustomers(IEventServerConnection eventSC)
			throws ConnectionException;

	public Customer retrieveCustomer(long customerid)
			throws ConnectionException;

	public void incrementNoPurchasesOfCustomer(long id)
			throws ConnectionException;

	public void updatePostcodeNoPurchasesRelation(String postcode,
			int noPurchases) throws ConnectionException;

	public void addPostcodeProductIDRelation(String postcode, int productid)
			throws ConnectionException;

}
