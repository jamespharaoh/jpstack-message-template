package shn.shopify.apiclient.customcollection;

import java.util.List;

import wbs.framework.logging.TaskLogger;

import shn.shopify.apiclient.ShopifyApiClientCredentials;

public
interface ShopifyCustomCollectionApiClient {

	List <ShopifyCustomCollectionResponse> listAll (
			TaskLogger parentTaskLogger,
			ShopifyApiClientCredentials credentials);

	ShopifyCustomCollectionResponse create (
			TaskLogger parentTaskLogger,
			ShopifyApiClientCredentials credentials,
			ShopifyCustomCollectionRequest request);

	ShopifyCustomCollectionResponse update (
			TaskLogger parentTaskLogger,
			ShopifyApiClientCredentials credentials,
			ShopifyCustomCollectionRequest request);

	void remove (
			TaskLogger parentTaskLogger,
			ShopifyApiClientCredentials credentials,
			Long id);

}