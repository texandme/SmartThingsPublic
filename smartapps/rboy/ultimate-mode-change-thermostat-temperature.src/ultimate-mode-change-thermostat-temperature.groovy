/*
 * -----------------------
 * ------ SMART APP ------
 * -----------------------
 *
 * STOP:  Do NOT PUBLISH the code to GitHub, it is a VIOLATION of the license terms.
 * You are NOT allowed share, distribute, reuse or publicly host (e.g. GITHUB) the code. Refer to the license details on our website.
 *
 */

/* **DISCLAIMER**
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
 * 1. the software will meet your requirements or expectations;
 * 2. the software or the software content will be free of bugs, errors, viruses or other defects;
 * 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
 * 4. the software will be compatible with third party software;
 * 5. any errors in the software will be corrected.
 * The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
 */ 

def clientVersion() {
    return "02.08.00"
}
 
/**
 *  Mode Change Thermostat Temperature
 *
 * Copyright RBoy Apps, redistribution or reuse of code is not allowed without permission
 *
 * 2019-02-04 - (v02.08.00) Enforce temperature settings unless Temp Hold is enabled to avoid accidental changes, fix for remote temp/door sensors not effective in single mode
 * 2019-01-28 - (v02.07.05) Implement permanent hold by selecting no thermostats
 * 2018-12-21 - (v02.07.04) Update max/min cooling/heating setpoints to be compatible with the Pearl Centralite thermostat
 * 2018-08-06 - (v02.07.02) Added option to save battery by reducing communications, disable if thermostat has losing commands in the mesh
 * 2018-07-30 - (v02.07.01) Added support for open door/window sensors to disable thermostat when opened
 * 2018-03-09 - (v02.06.00) Added option for temporary hold configuration and fix issue with mutiple thermostats using same settings
 * 2018-01-31 - (v02.05.05) Fix for temporary hold when thermostat reports decimal temperatures
 * 2018-01-28 - (v02.05.04) Print temperature instead null in messages (cosmetic issue)
 * 2017-11-13 - (v02.05.03) Optimization, don't loop through mode settings which aren't current
 * 2017-11-12 - (v02.05.02) Fix for remote temperature sensor with multiple modes
 * 2017-09-09 - (v02.05.01) Updated min temp to 60F for better GoControl thermostat compatibility
 * 2017-09-05 - (v02.05.00) Updated min/max thresholds for remote sensors to be compatible with new ST thermostat device handler (deadZones)
 * 2017-02-01 - (v2.4.1) Bugfix for temporary hold when using remote temperature sensors
 * 2017-01-27 - (v2.4.0) Added support for multiple remote temperature sensors per thermostat and for temporary hold mode for remote temperature sensor
 * 2016-11-05 - Added support for automatic code update notifications
 * 2016-04-23 - Fixed bug with settings in individual temperature for each thermostat for a single mode and various UI fixes
 * 2016-03-05 - Fixed bug with settings all modes temperature when in multi mode configuration mode
 * 2016-01-26 - Fixed a bug with modes selection
 * 2016-01-26 - Combined the single and individual temperature apps into a single app through a configurable menu
 * 2015-05-18 - Added support for individual mode temperatures
 * 2015-05-17 - Initial code
 *
 */
definition(
		name: "Ultimate Mode Change Thermostat Temperature",
		namespace: "rboy",
		author: "RBoy Apps",
		description: "Change the thermostat(s) temperature on a mode(s) change",
    	category: "Green Living",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
    	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png")

preferences {
	page(name: "setupApp")
    page(name: "tempPage")
}

def setupApp() {
    dynamicPage(name: "setupApp", title: "Ultimate Mode Change Thermostat Temperature v${clientVersion()}", install: false, uninstall: true, nextPage: "tempPage") {
        section("Select thermostat(s)") {
            input "thermostats", title: "Select thermostat(s) to configure", "capability.thermostat", required: false, multiple: true
        }

        section("Select Mode(s)") {
            input "modes", "mode", title: "Select mode(s) to configure", multiple: true, required: false
        }
        
        section("Advanced Settings", hidden: true, hideable: true) {
            input name: "batterySaver", type: "bool", title: "Save battery by skipping verification", description: "Disable this if schedules are not operating reliably", required: false, submitOnChange: false
        }

        section() {
            label title: "Assign a name for this SmartApp (optional)", required: false
            input name: "updateNotifications", title: "Check for new versions of the app", type: "bool", defaultValue: true, required: false
        }
    }
}

def tempPage() {
    dynamicPage(name:"tempPage", title: "Configure Temperature for modes and thermostats", uninstall: true, install: true) {
        section() {
            input name: "allowTempHold", type: "bool", title: "Allow thermostat temporary hold", description: "Allow the user to manually override the thermostat temperature setting until the next mode change", required: false, submitOnChange: false
            input name: "multiTempModes", type: "bool", title: "Separate temperatures for each mode", description: "Do you want to define different temperatures for each selected mode?", required: false, submitOnChange: true
            input name: "multiTempThermostat", type: "bool", title: "Separate temperatures for each thermostat", description: "Do you want to define different temperatures for each thermostat?", required: false, submitOnChange: true
        }
    	def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    	for (int j = 0; j < maxModes; j++) {
        	def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
            if (modeName != "All") {
                section() {
                    paragraph title: "$modeName Mode Thermostat Settings", "Enter the heat/cool temperatures for thermostats in $modeName mode"
                }
            }
            def maxThermostats = multiTempThermostat ? thermostats?.size() : 1
            for (int i = 0; i < maxThermostats; i++) {
                def heat = settings."opHeatSet${i}${j}"
                def cool = settings."opCoolSet${i}${j}"
                log.debug "$modeName Mode ${multiTempThermostat ? thermostats[i] : "All Thermostats"} Heat: $heat, Cool: $cool"

                section("${multiTempThermostat ? thermostats[i] : "All Thermostats"} heat/cool temperatures") {
                    input "opHeatSet${i}${j}", "decimal", defaultValue: "${heat ?: ""}", title: "When Heating", description: "Heating temperature for mode", required: true
                    input "opCoolSet${i}${j}", "decimal", defaultValue: "${cool ?: ""}", title: "When Cooling", description: "Cooling temperature for mode", required: true
                    if ((settings."remoteTemperatureSensor${i}${j}"*.currentTemperature)?.count { it } > 1) {
                        paragraph title: "You have selected multiple remote sensors, the average temperature across the sensors will be used", required: true, ""
                    }
                    input "remoteTemperatureSensor${i}${j}", "capability.temperatureMeasurement", title: "Remote temperature sensor", description: "Use remote temperature sensor to control ${multiTempThermostat ? thermostats[i] : "All Thermostats"} for $modeName mode", required: false, multiple:true, submitOnChange: true
                    if (settings."remoteTemperatureSensor${i}${j}") {
                        input "threshold${i}${j}", "decimal", title: "Temperature swing (precision)", defaultValue: "1.0", required: true // , range: "0.5..5.0" causes Android 2.0.7 to crash, TODO: add this in later
                    }
                    input "openDoors${i}${j}", "capability.contactSensor", title: "Open door/window sensor(s)", description: "Shutdown thermostat if any of these are opened", required: false, multiple:true, submitOnChange: false
                }
            }
        }
    }
}

// Globals
private getMIN_HEAT_TEMP_F() { 45 } // Keep a deadband diff between heat and cool
private getMAX_HEAT_TEMP_F() { 84 }
private getMIN_COOL_TEMP_F() { 60 }
private getMAX_COOL_TEMP_F() { 86 }
private getMIN_HEAT_TEMP_C() { 8 }
private getMAX_HEAT_TEMP_C() { 28 }
private getMIN_COOL_TEMP_C() { 15 }
private getMAX_COOL_TEMP_C() { 30 }

def installed()
{
	subscribeToEvents()
}

def updated()
{
    unsubscribe()
    unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
    // Check for new versions of the code
    def random = new Random()
    Integer randomHour = random.nextInt(18-10) + 10
    Integer randomDayOfWeek = random.nextInt(7-1) + 1 // 1 to 7
    schedule("0 0 " + randomHour + " ? * " + randomDayOfWeek, checkForCodeUpdate) // Check for code updates once a week at a random day and time between 10am and 6pm

    def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    for (int j = 0; j < maxModes; j++) {
        def maxThermostats = multiTempThermostat ? thermostats?.size() : 1
        for (int i = 0; i < maxThermostats; i++) {
            subscribe(settings."remoteTemperatureSensor${i}${j}", "temperature", remoteChangeHandler) // Handle changes in remote temperature sensor and readjust thermostat
            subscribe(settings."openDoors${i}${j}", "contact", doorContactHandler) // Handle open/closed door/windows contact sensors
        }
    }

    subscribe(thermostats, "heatingSetpoint", thermostatSetTempHandler) // Handle changes in manual thermostat heating setpoint temperature changes for Hold mode
    subscribe(thermostats, "coolingSetpoint", thermostatSetTempHandler) // Handle changes in manual thermostat cooling setpoint temperature changes for Hold mode
    subscribe(location, modeChangeHandler)
    
    log.debug "Selected Modes -> $modes, settings -> $settings"
    
    // Kick start if we are in the right mode
    modeChangeHandler([value:location.mode])
}

// Handle events from contact sensors from doors/windows
def doorContactHandler(evt) {
    log.debug "Received door/window contact notification from ${evt.device.displayName}, name: ${evt.name}, value: ${evt.value}"
    
    def msgs = []

	// Since we are manually entering the mode, check the mode before continuing since these aren't registered with the system (thanks @bravenel)
    if (modes && !modes.contains(location.mode)) {
    	log.trace "${location.mode} mode not in list of selected modes $modes"
    	return
    }
    
    // Lets find which thermostats are linked to this sensor and then update thermostat settings accordingly
    def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    for (int j = 0; j < maxModes; j++) {
    	def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
        if (!((modeName == location.mode) || (modeName == "All"))) { // check for matching mode in loop
            continue // Not our mode
        }
        def maxThermostats = thermostats?.size()
        for (int count = 0; count < maxThermostats; count++) {
            int i = multiTempThermostat ? count : 0
            if(settings."openDoors${i}${j}"*.id?.contains(evt.device.id)) {
                if (evt.value == "open") { // When sensor is opened, disabled HVAC/appliances
                    atomicState."openDoors${i}${j}" = true // We have an open door
                    atomicState."holdTemp${thermostats[count]}" = false // Reset temp hold
                    def msg = "${evt.device.displayName} opened, turning off ${thermostats[count].displayName}"
                    log.debug msg
                    msgs << msg
                } else if ((evt.value == "closed") && settings."openDoors${i}${j}".every { it.currentValue("contact") == "closed" }) { // When all sensors are closed, reset the the HVAC/appliances settings as per schedule
                    atomicState."openDoors${i}${j}" = false // Reset it
                    def msg = "All door/window sensors are closed, resuming HVAC/applicance operation"
                    log.debug msg
                    msgs << msg
                }

                setActiveTemperature(thermostats[count], i, j) // Update thermostat state
            }
        }
    }
    
    msgs.each { msg ->
        sendNotificationEvent(msg) // Do it in the end to avoid a timeout
    }
}

// Handle manual changes in thermostat setpoint for temporary Hold mode when using remote sensors
def thermostatSetTempHandler(evt) {
    log.debug "Received temperature set notification from ${evt.device.displayName}, name: ${evt.name}, value: ${evt.value}, mode: ${location.mode}"

	// Since we are manually entering the mode, check the mode before continuing since these aren't registered with the system (thanks @bravenel)
    if (modes && !modes.contains(location.mode)) {
    	log.trace "${location.mode} mode not in list of selected modes $modes"
    	return
    }
    
    // Lets find which remote sensors are linked to this thermostat and see if we are in hold more or not
    def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    for (int j = 0; j < maxModes; j++) {
        def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
        if (!((modeName == location.mode) || (modeName == "All"))) { // check for matching mode in loop
            continue // this isn't our mode, ignore it
        }
        def maxThermostats = thermostats?.size()
        for (int count = 0; count < maxThermostats; count++) {
            int i = multiTempThermostat ? count : 0
            if (thermostats[count].id.contains(evt.device.id)) { // Find the thermostat which reported this change
                def remoteTemperatureSensor = settings."remoteTemperatureSensor${i}${j}"
                def coolingSetpoint = settings."opCoolSet${i}${j}"
                def heatingSetpoint = settings."opHeatSet${i}${j}"
                def locationScale = getTemperatureScale()
                def maxCTemp
                def minCTemp
                def maxHTemp
                def minHTemp
                if (locationScale == "C") {
                    minCTemp = MIN_COOL_TEMP_C // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
                    maxCTemp = MAX_COOL_TEMP_C // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
                    minHTemp = MIN_HEAT_TEMP_C // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
                    maxHTemp = MAX_HEAT_TEMP_C // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
                    log.trace "Location is in Celsius, MaxHeatTemp $maxHTemp, MinHeatTemp $minHTemp, MaxCoolTemp $maxCTemp, MinCoolTemp $minCTemp for thermostat"
                } else {
                    minCTemp = MIN_COOL_TEMP_F // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
                    maxCTemp = MAX_COOL_TEMP_F // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
                    minHTemp = MIN_HEAT_TEMP_F // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
                    maxHTemp = MAX_HEAT_TEMP_F // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
                    log.trace "Location is in Farenheit, MaxHeatTemp $maxHTemp, MinHeatTemp $minHTemp, MaxCoolTemp $maxCTemp, MinCoolTemp $minCTemp for thermostat"
                }

                // Check if the see temperature is any of the set heat/cold or mix/min settings, otherwise it's a manual adjustment and we should be in temporary hold mode
                if (!allowTempHold) {
                    if (!(evt.name == "coolingSetpoint" ? [coolingSetpoint, minCTemp, maxCTemp] : [heatingSetpoint, minHTemp, maxHTemp]).any { (it as Float) == (evt.value as Float) }) {
                        def msg = "${app.label}: Temporary hold feature not enabled, ignoring ${evt.device.displayName} ${evt.name} override ${evt.value}°${remoteTemperatureSensor ? '. Using ' + remoteTemperatureSensor + ' remote temperature sensor' : ''}"
                        atomicState."holdTemp${evt.device}" = false
                        setActiveTemperature(thermostats[count], i, j) // Reset setpoints
                        log.warn msg
                        sendNotificationEvent(msg) // Do it in the end to avoid a timeout
                        return // We're done
                    }
                } else if ((evt.name == "coolingSetpoint" ? [coolingSetpoint, minCTemp, maxCTemp] : [heatingSetpoint, minHTemp, maxHTemp]).any { (it as Float) == (evt.value as Float) }) {
                    log.trace "${remoteTemperatureSensor ? 'Found ' + remoteTemperatureSensor + ' remote temperature sensor connected to thermostat. ' : ''}Thermostat ${evt.device.displayName} set to predefined setpoint ${evt.value} for ${evt.name} in mode ${modeName}, disabling temporary hold"
                    atomicState."holdTemp${evt.device}" = false
                } else {
                    log.info "${remoteTemperatureSensor ? 'Found ' + remoteTemperatureSensor + ' remote temperature sensor connected to thermostat. ' : ''}Thermostat ${evt.device.displayName} set to with manual setpoint ${evt.value} for ${evt.name} in mode ${modeName}, enabling temporary hold"
                    atomicState."holdTemp${evt.device}" = true
                }
            }
        }
    }
}

// Handle remote temp sensor, set temperature if using a remote sensor
def remoteChangeHandler(evt) {
    log.debug "Reinitializing thermostat on remote sensor ${evt?.device?.displayName} temp change notification, name: ${evt?.name}, value: ${evt?.value}, mode: ${location.mode}"

	// Since we are manually entering the mode, check the mode before continuing since these aren't registered with the system (thanks @bravenel)
    if (modes && !modes.contains(location.mode)) {
    	log.trace "${location.mode} mode not in list of selected modes $modes"
    	return
    }
    
    // Lets find which thermostats are linked to this sensor and then update thermostat settings accordingly
    def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    for (int j = 0; j < maxModes; j++) {
        def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
        if (!((modeName == location.mode) || (modeName == "All"))) { // check for matching mode in loop
            continue // this isn't our mode, ignore it
        }
        def maxThermostats = thermostats?.size()
        for (int count = 0; count < maxThermostats; count++) {
            int i = multiTempThermostat ? count : 0
            if(settings."remoteTemperatureSensor${i}${j}"*.id?.contains(evt.device.id)) {
                if (atomicState."holdTemp${thermostats[count]}") { // If we are on hold temp mode then ignore temperature changes since user has put it on hold mode
                    log.trace "Thermostat ${thermostats[count]} is in hold temperature mode, not making any changes to thermostat based on remote temp sensor"
                } else {
                    setActiveTemperature(thermostats[count], i, j)
                }
            }
        }
    }
}

// Set the active thermostat temperature on thermostat
private setActiveTemperature(thermostat, i, j) {
    def openDoor = atomicState."openDoors${i}${j}"
    def coolingSetpoint = settings."opCoolSet${i}${j}"
    def heatingSetpoint = settings."opHeatSet${i}${j}"
    def thermostatState = thermostat.latestValue("thermostatMode")
    def thermostatCurrentHeating = thermostat.currentValue("heatingSetpoint")
    def thermostatCurrentCooling = thermostat.currentValue("coolingSetpoint")
    def threshold = settings."threshold${i}${j}"
    def remoteTemperatureSensor = settings."remoteTemperatureSensor${i}${j}"

    if (!thermostat) {
        log.error "No thermostat selected, not doing anything"
        return
    }
    
    log.trace "Thermostat ${thermostat.displayName} mode: $thermostatState, Target Heat: $heatingSetpoint°, Target Cool: $coolingSetpoint°"

    // Check for invalid configuration
    if ((thermostatState == "auto") && (heatingSetpoint > coolingSetpoint)) {
        def msg = "INVALID CONFIGURATION: Target Heat temperature: $heatingSetpoint° is GREATER than Target Cool temperature: $coolingSetpoint°\nNot changing temperature settings on thermostat, correct the SmartApp settings"
        log.error msg
        sendPush(msg)
        return
    }

    def locationScale = getTemperatureScale()
    def maxCTemp
    def minCTemp
    def maxHTemp
    def minHTemp
    if (locationScale == "C") {
        minCTemp = MIN_COOL_TEMP_C // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
        maxCTemp = MAX_COOL_TEMP_C // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
        minHTemp = MIN_HEAT_TEMP_C // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
        maxHTemp = MAX_HEAT_TEMP_C // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
        log.trace "Location is in Celsius, MaxHeatTemp $maxHTemp, MinHeatTemp $minHTemp, MaxCoolTemp $maxCTemp, MinCoolTemp $minCTemp for thermostat"
    } else {
        minCTemp = MIN_COOL_TEMP_F // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
        maxCTemp = MAX_COOL_TEMP_F // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
        minHTemp = MIN_HEAT_TEMP_F // minimum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to minimum)
        maxHTemp = MAX_HEAT_TEMP_F // maximum temperature (thermostat could be in a different room at different temp or not updated very often so inaccurate hence to set to maximum)
        log.trace "Location is in Farenheit, MaxHeatTemp $maxHTemp, MinHeatTemp $minHTemp, MaxCoolTemp $maxCTemp, MinCoolTemp $minCTemp for thermostat"
    }

    if (remoteTemperatureSensor) { // Remote temperature sensor
        def currentTemp = (remoteTemperatureSensor*.currentTemperature).sum()/(remoteTemperatureSensor*.currentTemperature).count { it } // Take the average temp of the remote temperature sensor(s) (manage transition from legacy code to new code)
        log.trace("Remote Sensor Current Temp: $currentTemp°, Swing Threshold: $threshold")

        if (thermostatState == "auto") {
            // Order is important, Shutdown, Cooling, Heating and last Turn Off
            if (openDoor) { // Check if we need to shutdown
                if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                    thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                }
                if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                    thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                }
                log.info "Open sensor: $thermostat OFF, Thermostat Cool: ${maxCTemp}, Thermostat Heat: ${minHTemp}"
            } else if ((currentTemp - coolingSetpoint) > threshold) { // Turn cool on first
                if (!batterySaver || thermostatCurrentCooling != minCTemp) {
                    thermostat.setCoolingSetpoint(minCTemp) // Set to cool
                }
                if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                    thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                }
                log.info "$thermostat Cooling ON, Thermostat Cool: ${minCTemp}, Target: $coolingSetpoint°"
            } else if ((heatingSetpoint - currentTemp) > threshold) { // Heating second (order is important to avoid constant switching)
                if (!batterySaver || thermostatCurrentHeating != maxHTemp) {
                    thermostat.setHeatingSetpoint(maxHTemp) // Set to heat
                }
                if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                    thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                }
                log.info "$thermostat Heating ON, Thermostat Heat: ${maxHTemp}, Target: $heatingSetpoint°"
            } else if (((coolingSetpoint - currentTemp) > threshold) || ((currentTemp - heatingSetpoint) > threshold)) { // Turn off - don't check valid mode
                if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                    thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                }
                if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                    thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                }
                log.info "$thermostat OFF, Thermostat Cool: ${maxCTemp}, Thermostat Heat: ${minHTemp}"
            }
        } else if (thermostatState == "cool") {
            // Order is important, Shutdown, Cooling, Heating and last Turn Off
            if (openDoor) { // Check if we need to shutdown
                if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                    thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                }
                log.info "Open sensor: $thermostat Cooling OFF, Thermostat Cool: ${maxCTemp}"
            } else if ((currentTemp - coolingSetpoint) > threshold) { // Turn cool on
                if (!batterySaver || thermostatCurrentCooling != minCTemp) {
                    thermostat.setCoolingSetpoint(minCTemp) // Set to cool
                }
                log.info "$thermostat Cooling ON, Thermostat Cool: ${minCTemp}, Target: $coolingSetpoint°"
            } else if (((coolingSetpoint - currentTemp) > threshold)) { // Turn cool off - don't check valid mode
                if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                    thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                }
                log.info "$thermostat Cooling OFF, Thermostat Cool: ${maxCTemp}"
            }
        } else { // Heater or emergency heater
            // Order is important, Shutdown, Cooling, Heating and last Turn Off
            if (openDoor) { // Check if we need to shutdown
                if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                    thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                }
                log.info "Open sensor: $thermostat Heating OFF, Thermostat Heat: ${minHTemp}"
            } else if ((heatingSetpoint - currentTemp) > threshold) {
                if (!batterySaver || thermostatCurrentHeating != maxHTemp) {
                    thermostat.setHeatingSetpoint(maxHTemp) // Set to heat
                }
                log.info "$thermostat Heating ON, Thermostat Heat: ${maxHTemp}, Target: $heatingSetpoint°"
            } else if (((currentTemp - heatingSetpoint) > threshold)) { // Disable heat - don't check valid mode
                if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                    thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                }
                log.info "$thermostat Heating OFF, Thermostat Heat: ${minHTemp}"
            }
        }
    } else { // Local thermostat
        // Order is important, Shutdown, Cooling, Heating and last Turn Off
        if (openDoor) { // Check if we need to shutdown
            switch (thermostatState) {
                case "auto":
                    if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                        thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                    }
                    if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                        thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                    }
                    log.info "Open sensor turning $thermostat OFF, Thermostat Cool: ${maxCTemp}, Thermostat Heat: ${minHTemp}"
                    break

                case "cool":
                    if (!batterySaver || thermostatCurrentCooling != maxCTemp) {
                        thermostat.setCoolingSetpoint(maxCTemp) // Disable cool
                    }
                    log.info "Open sensor turning $thermostat OFF, Thermostat Cool: ${maxCTemp}"
                    break

                case "heat":
                    if (!batterySaver || thermostatCurrentHeating != minHTemp) {
                        thermostat.setHeatingSetpoint(minHTemp) // Disable heat
                    }
                    log.info "Open sensor turning $thermostat OFF, Thermostat Heat: ${minHTemp}"
                    break

                default:
                    break
            }
        } else if (thermostatState == "auto") {
            def msg = ""
            if (!batterySaver || thermostatCurrentHeating != heatingSetpoint) {
                thermostat.setHeatingSetpoint(heatingSetpoint)
                msg += "Set $thermostat Heat ${heatingSetpoint}°"
            }
            if (!batterySaver || thermostatCurrentCooling != coolingSetpoint) {
                thermostat.setCoolingSetpoint(coolingSetpoint)
                if (msg) {
                    msg += "Set $thermostat Cool ${coolingSetpoint}°"
                } else {
                    msg += ", Cool ${coolingSetpoint}°"
                }
            }
            if (msg) {
                log.info msg
            }
        } else if (thermostatState == "cool") {
            if (!batterySaver || thermostatCurrentCooling != coolingSetpoint) {
                thermostat.setCoolingSetpoint(coolingSetpoint)
                log.info "Set $thermostat Cool ${coolingSetpoint}°"
            }
        } else { // heater or emergency heater
            if (!batterySaver || thermostatCurrentHeating != heatingSetpoint) {
                thermostat.setHeatingSetpoint(heatingSetpoint)
                log.info "Set $thermostat Heat ${heatingSetpoint}°"
            }
        }
    }
}

// Handle mode changes, reinitialize the current temperature after a mode change
def modeChangeHandler(evt) {
    def msgs = []
    
	// Since we are manually entering the mode, check the mode before continuing since these aren't registered with the system (thanks @bravenel)
    if (modes && !modes.contains(evt.value)) {
    	log.trace "$evt.value mode not in list of selected modes $modes"
    	return
    }
    
    thermostats?.each { atomicState."holdTemp${it}" = false } // Reset temporary hold mode
    
    def maxModes = multiTempModes ? (modes == null ? 1 : modes.size()) : 1
    int j = 0
    for (j = 0; j < maxModes; j++) {
    	def modeName = multiTempModes ? (modes == null ? "All" : modes[j]) : "All"
        if ((modeName == evt.value) || (modeName == "All")) { // check for matching mode in loop
            break // got it
        }
    }

    def maxThermostats = thermostats?.size()
    for (int count = 0; count < maxThermostats; count++) {
        int i = multiTempThermostat ? count : 0
        def coolingSetpoint = settings."opCoolSet${i}${j}"
        def heatingSetpoint = settings."opHeatSet${i}${j}"
        def msg = "Set ${thermostats[count]} Heat ${heatingSetpoint}°, Cool ${coolingSetpoint}° on ${evt.value} mode"
        log.info msg
        msgs << msg
        setActiveTemperature(thermostats[count], i, j)
    }
    
    msgs.each { msg ->
        sendNotificationEvent(msg) // Do it in the end to avoid a timeout
    }
}

def checkForCodeUpdate(evt) {
    log.trace "Getting latest version data from the RBoy Apps server"
    
    def appName = "Ultimate Mode Change Thermostat Temperature"
    def serverUrl = "http://smartthings.rboyapps.com"
    def serverPath = "/CodeVersions.json"
    
    try {
        httpGet([
            uri: serverUrl,
            path: serverPath
        ]) { ret ->
            log.trace "Received response from RBoy Apps Server, headers=${ret.headers.'Content-Type'}, status=$ret.status"
            //ret.headers.each {
            //    log.trace "${it.name} : ${it.value}"
            //}

            if (ret.data) {
                log.trace "Response>" + ret.data
                
                // Check for app version updates
                def appVersion = ret.data?."$appName"
                if (appVersion > clientVersion()) {
                    def msg = "New version of app ${app.label} available: $appVersion, current version: ${clientVersion()}.\nPlease visit $serverUrl to get the latest version."
                    log.info msg
                    if (updateNotifications != false) { // The default true may not be registered
                        sendPush(msg)
                    }
                } else {
                    log.trace "No new app version found, latest version: $appVersion"
                }
                
                // Check device handler version updates
                def caps = [ thermostats ]
                caps?.each {
                    def devices = it?.findAll { it.hasAttribute("codeVersion") }
                    for (device in devices) {
                        if (device) {
                            def deviceName = device?.currentValue("dhName")
                            def deviceVersion = ret.data?."$deviceName"
                            if (deviceVersion && (deviceVersion > device?.currentValue("codeVersion"))) {
                                def msg = "New version of device handler for ${device?.displayName} available: $deviceVersion, current version: ${device?.currentValue("codeVersion")}.\nPlease visit $serverUrl to get the latest version."
                                log.info msg
                                if (updateNotifications != false) { // The default true may not be registered
                                    sendPush(msg)
                                }
                            } else {
                                log.trace "No new device version found for $deviceName, latest version: $deviceVersion, current version: ${device?.currentValue("codeVersion")}"
                            }
                        }
                    }
                }
            } else {
                log.error "No response to query"
            }
        }
    } catch (e) {
        log.error "Exception while querying latest app version: $e"
    }
}


// THIS IS THE END OF THE FILE