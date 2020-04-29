package org.sil.storyproducer.model

import android.content.Context
import com.crashlytics.android.Crashlytics
import org.json.JSONException
import org.json.JSONObject
import org.sil.storyproducer.tools.file.getChildOutputStream
import org.sil.storyproducer.tools.file.getText
import java.util.*

const val REGISTRATION_FILENAME = "registration.json"

fun registrationTimeStamp(): String {
    val calendar: Calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString()
    val month = (calendar.get(Calendar.MONTH) + 1).toString()
    val year = calendar.get(Calendar.YEAR).toString()
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString()
    var min = calendar.get(Calendar.MINUTE).toString()
    if (min.length < 2) {
        min = "0$min"
    }
    return "$month/$day/$year $hour:$min"
}

class Registration {

    var date = registrationTimeStamp()

    var projectLanguage = ""
    var projectEthnoCode = ""
    var projectCountry = ""
    var projectRegion = ""
    var projectCity = ""
    var projectMajorityLanguage = ""
    var projectOrthography = ""

    var translatorName = ""
    var translatorEducation = ""
    var translatorLanguages = ""
    var translatorPhone = ""
    var translatorEmail = ""
    var translatorCommunicationPreference = ""
    var translatorLocation = ""

    var consultantName = ""
    var consultantLanguages = ""
    var consultantPhone = ""
    var consultantEmail = ""
    var consultantCommunicationPreference = ""
    var consultantLocation = ""
    var consultantLocationType = ""

    var trainerName = ""
    var trainerLanguages = ""
    var trainerPhone = ""
    var trainerEmail = ""
    var trainerCommunicationPreference = ""
    var trainerLocation = ""

    var archiveEmail1 = ""
    var archiveEmail2 = ""
    var archiveEmail3 = "SPapp_info@sil.org"

    var registrationComplete = false
    var registrationEmailSent = false

    fun load(context: Context) {
        val regString: String? = getText(context, REGISTRATION_FILENAME)
        if (regString != null) {
            try {
                val obj = JSONObject(regString)

                date = obj.optString("date", registrationTimeStamp())

                projectLanguage = obj.optString("language", "")
                projectEthnoCode = obj.optString("ethnologue", "")
                projectCountry = obj.optString("country", "")
                projectRegion = obj.optString("location", "")
                projectCity = obj.optString("town", "")
                projectMajorityLanguage = obj.optString("lwc", "")
                projectOrthography = obj.optString("orthography", "")

                translatorName = obj.optString("translator_name", "")
                translatorEducation = obj.optString("translator_education", "")
                translatorLanguages = obj.optString("translator_languages", "")
                translatorPhone = obj.optString("translator_phone", "")
                translatorEmail = obj.optString("translator_email", "")
                translatorCommunicationPreference = obj.optString("translator_communication_preference", "")
                translatorLocation = obj.optString("translator_location", "")

                consultantName = obj.optString("consultant_name", "")
                consultantLanguages = obj.optString("consultant_languages", "")
                consultantPhone = obj.optString("consultant_phone", "")
                consultantEmail = obj.optString("consultant_email", "")
                consultantCommunicationPreference = obj.optString("consultant_communication_preference", "")
                consultantLocation = obj.optString("consultant_location", "")
                consultantLocationType = obj.optString("consultant_location_type", "")

                trainerName = obj.optString("trainer_name", "")
                trainerLanguages = obj.optString("trainer_languages", "")
                trainerPhone = obj.optString("trainer_phone", "")
                trainerEmail = obj.optString("trainer_email", "")
                trainerCommunicationPreference = obj.optString("trainer_communication_preference", "")
                trainerLocation = obj.optString("trainer_location", "")

                archiveEmail1 = obj.optString("database_email_1", "")
                archiveEmail2 = obj.optString("database_email_2", "")
                archiveEmail3 = obj.optString("database_email_3", "")
                if (archiveEmail3 == "") {
                    archiveEmail3 = "SPapp_info@sil.org"
                }

                registrationComplete = obj.optBoolean("registration_complete", false)
                registrationEmailSent = obj.optBoolean("registration_email_sent", false)
            } catch (e: JSONException) {
                Crashlytics.logException(e)
            }
        }
    }

    fun save(context: Context) {
        val oStream = getChildOutputStream(context, REGISTRATION_FILENAME, "")
        if (oStream != null) {
            try {
                val obj = JSONObject()

                obj.put("date", date)

                obj.put("language", projectLanguage)
                obj.put("ethnologue", projectEthnoCode)
                obj.put("country", projectCountry)
                obj.put("location", projectRegion)
                obj.put("town", projectCity)
                obj.put("lwc", projectMajorityLanguage)
                obj.put("orthography", projectOrthography)

                obj.put("translator_name", translatorName)
                obj.put("translator_education", translatorEducation)
                obj.put("translator_languages", translatorLanguages)
                obj.put("translator_phone", translatorPhone)
                obj.put("translator_email", translatorEmail)
                obj.put("translator_communication_preference", translatorCommunicationPreference)
                obj.put("translator_location", translatorLocation)

                obj.put("consultant_name", consultantName)
                obj.put("consultant_languages", consultantLanguages)
                obj.put("consultant_phone", consultantPhone)
                obj.put("consultant_email", consultantEmail)
                obj.put("consultant_communication_preference", consultantCommunicationPreference)
                obj.put("consultant_location", consultantLocation)
                obj.put("consultant_location_type", consultantLocationType)

                obj.put("trainer_name", trainerName)
                obj.put("trainer_languages", trainerLanguages)
                obj.put("trainer_phone", trainerPhone)
                obj.put("trainer_email", trainerEmail)
                obj.put("trainer_communication_preference", trainerCommunicationPreference)
                obj.put("trainer_location", trainerLocation)

                obj.put("database_email_1", archiveEmail1)
                obj.put("database_email_2", archiveEmail2)
                obj.put("database_email_3", archiveEmail3)

                obj.put("registration_complete", registrationComplete)
                obj.put("registration_email_sent", registrationEmailSent)


                oStream.write(obj.toString(2).toByteArray(Charsets.UTF_8))
            } catch (e: Exception) {
                Crashlytics.logException(e)
            } finally {
                oStream.close()
            }
        }
    }
}


