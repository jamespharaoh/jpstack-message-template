package shn.shopify.apiclient.metafield;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.framework.data.annotations.DataChild;
import wbs.framework.data.annotations.DataClass;

import shn.shopify.apiclient.ShopifyApiClientCredentials;
import shn.shopify.apiclient.ShopifyApiRequest;
import shn.shopify.apiclient.ShopifyApiResponse;
import wbs.web.misc.HttpMethod;

@Accessors (fluent = true)
@Data
@DataClass
public
class ShopifyMetafieldUpdateRequest
	implements ShopifyApiRequest {

	ShopifyApiClientCredentials httpCredentials;

	@DataChild (
		name = "metafield")
	ShopifyMetafieldRequest metafield;

	// shopify api request implementation

	@Override
	public
	HttpMethod httpMethod () {
		return HttpMethod.put;
	}

	@Override
	public
	String httpPath () {

		return stringFormat (
			"/admin/metafields/%s.json",
			integerToDecimalString (
				metafield.id ()));

	}

	@Override
	public
	Class <? extends ShopifyApiResponse> httpResponseClass () {
		return ShopifyMetafieldUpdateResponse.class;
	}

}