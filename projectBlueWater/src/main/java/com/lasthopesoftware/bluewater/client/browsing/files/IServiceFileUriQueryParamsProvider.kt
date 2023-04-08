package com.lasthopesoftware.bluewater.client.browsing.files

/**
 * Created by david on 3/26/17.
 */
interface IServiceFileUriQueryParamsProvider {
    fun getServiceFileUriQueryParams(serviceFile: ServiceFile): Array<String>
}
