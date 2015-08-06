/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Backend with Google Cloud Messaging" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/GcmEndpoints
*/

package com.bgdev.out.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import static com.bgdev.out.backend.OfyService.ofy;

/**
 * A registration endpoint class we are exposing for a device's GCM registration id on the backend
 * <p/>
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 * <p/>
 * NOTE: This endpoint does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Api(name = "registration", version = "v2", namespace = @ApiNamespace(ownerDomain = "backend.out.bgdev.com", ownerName = "backend.out.bgdev.com", packagePath = ""))
public class RegistrationEndpoint {

    /**
     * Register a device to the backend
     *
     * @param regId The Google Cloud Messaging registration Id to add
     */
    @ApiMethod(name = "register",httpMethod = ApiMethod.HttpMethod.POST)
    public RegistrationRecord registerDevice(@Named("regId") String regId, @Named("uniqId") Long uniqId) {
        RegistrationRecord rec = findGcmRecord(regId);

        if (rec==null){
            rec = new RegistrationRecord();
            rec.setUserUniqId(uniqId);
            rec.setRegId(regId);
        }
        else {
            if (!uniqId.equals(rec.getUserUniqId())) {
                rec.setUserUniqId(uniqId);}
        }

        ofy().save().entity(rec).now();
        return rec;
    }

    /**
     * Unregister a device from the backend
     *
     * @param regId The Google Cloud Messaging registration Id to remove
     */
    @ApiMethod(name = "unregister",httpMethod = ApiMethod.HttpMethod.DELETE)
    public void unregisterDevice(@Named("regId") String regId) {
        RegistrationRecord record = findGcmRecord(regId);
        if (record == null) {
            return;
        }
        ofy().delete().entity(record).now();
    }

    public static RegistrationRecord findGcmRecord(String regId){
        return ofy().load().type(RegistrationRecord.class).filter("regId",regId).first().now();
    }

    public static List<String> findRecordsBasedOnId(Long id){
        List<String> regIdList = new ArrayList<>();
        List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).filter("userUniqId",id).list();
        for (int i = 0; i < records.size(); i++){
            regIdList.add(records.get(i).getRegId());
        }
        return regIdList;
    }

    public static void addRegIdsToList(Long id,List<String> returnList){
        List<String> regIds = findRecordsBasedOnId(id);
        for (int i = 0; i <regIds.size();i++){
            returnList.add(regIds.get(i));
        }
    }



}
