/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/apps/wyzehub-app.groovy
 *
 * DON'T BE A DICK PUBLIC LICENSE
 *
 * Version 1.1, December 2016
 *
 * Copyright (C) 2021 Jake Lehner
 * 
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document.
 * 
 * DON'T BE A DICK PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 1. Do whatever you like with the original work, just don't be a dick.
 * 
 *    Being a dick includes - but is not limited to - the following instances:
 *
 *    1a. Outright copyright infringement - Don't just copy this and change the name.
 *    1b. Selling the unmodified original with no work done what-so-ever, that's REALLY being a dick.
 *    1c. Modifying the original work to contain hidden harmful content. That would make you a PROPER dick.
 *
 * 2. If you become rich through modifications, related works/services, or supporting the original work,
 *    share the love. Only a dick would make loads off this work and not buy the original work's
 *    creator(s) a pint.
 * 
 * 3. Code is provided with no warranty. Using somebody else's code and bitching when it goes wrong makes
 *    you a DONKEY dick. Fix the problem yourself. A non-dick would submit the fix back.
 *
 * ===================================================================================
 * 
 * Release Notes:
 *   v1.3 - Address issue with refresh token logic.
 *        - Add Light Strip Support (non-pro)
 *   v1.2 - Bugfix allowing Meshlight to be used with rules engine (thanks @bruderbell!)
 *        - Add Camera Event Recording Enable/Disable (thanks @fieldsjm!)
 *   v1.1 - Add Camera Driver (thanks @fieldsjm!)
 *        - Add Outdoor Plug
 *        - Hubitat Package Manager
 *   v1.0 - Initial Release. 
 *        - Support for Color Bulbs, Plugs, and associated groups.
 *  
 */

import groovy.json.JsonBuilder
import groovy.transform.Field
import java.security.MessageDigest
import static java.util.UUID.randomUUID

public static final String version() { return "v1.4" }

public static final String apiAppName() { return "com.hualai" }
public static final String apiAppVersion() { return "2.19.14" }

@Field static final String childNamespace = "jakelehner" 

@Field static final Map groupDriverMap = [
    1: [label: 'Camera Group', driver: 'WyzeHub Camera Group'],
	2: [label: 'Bulb Group', driver: 'WyzeHub Bulb Group'],
    5: [label: 'Plug Group', driver: 'WyzeHub Plug Group'],
    8: [label: 'Color Bulb Group', driver: 'WyzeHub Color Bulb Group'],
]
@Field static final List ignoreDeviceModels = [
	'WLPPO'
]
@Field static final Map driverMap = [
	'Light': [label: 'Bulb', driver: 'WyzeHub Bulb'],
	'MeshLight': [label: 'Color Bulb', driver: 'WyzeHub Color Bulb'],
	'LightStrip' : [label: 'Light Strip', driver: 'WyzeHub Color Bulb'],
	'Plug': [label: 'Plug', driver: 'WyzeHub Plug'],
	'OutdoorPlug': [label: 'Outdoor Plug', driver: 'WyzeHub Plug'],
	'Camera': [label: 'Camera', driver: 'WyzeHub Camera'],
	'default': [label: 'Unsupported Type', driver: null]
]

@Field static final String log_level_debug   = '5'
@Field static final String log_level_info    = '4'
@Field static final String log_level_notice  = '3'
@Field static final String log_level_warn    = '2'
@Field static final String log_level_error   = '1'
@Field static final String log_level_off     = '0'
@Field static final String log_level_default = '4'

String wyzeAuthBaseUrl() { return "https://auth-prod.api.wyze.com" }
String wyzeApiBaseUrl() { return "https://api.wyzecam.com" }

Map wyzeRequestHeaders() {
    return [
        "x-api-key": "WMXHYf79Nr5gIlt3r0r7p9Tcw5bvs6BB4U8O8nGJ",
        "Content-Type": "application/json",
		"Accept": "application/json",
		"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1 Safari/605.1.15"
    ]
}

Map wyzeRequestBody() {
    return [
        'access_token': state.access_token,
		'app_name': apiAppName(),
		'app_ver': apiAppName() + "_v" + apiAppVersion(),
		'app_version': apiAppVersion(),
		'phone_id': getPhoneId(),
		'phone_system_type': 2,
		'sc': '9f275790cab94a72bd206c8876429f3c',
		'ts': (new Date()).getTime()
    ]
}

definition(
	name: 'WyzeHub',
	namespace: 'jakelehner',
	author: 'Jake Lehner',
	description: 'Hubitat Integration for Wyze WiFi devices.',
	iconUrl: '',
	iconX2Url: '',
	iconX3Url: '',
	installOnOpen: true,
	singleInstance: true,
	oauth: false,
	importUrl: 'https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/apps/wyzehub-app.groovy'
)

preferences 
{
   page(name: 'pageMenu')
   page(name: 'pageAuthSettings')
   page(name: 'pageDoAuth')
   page(name: 'pageDoMfaAuth')
   page(name: 'pageSelectDevices')
   page(name: 'pageSelectDeviceGroups')
}

//  ---------------------
// | App Control Methods |
//  ---------------------

def installed() 
{
	logDebug('installed()')

	clearState()	
	initialize()
	state.serverInstalled = true

	logInfo('App Installed')
}

def updated() 
{
	logDebug('updated()')
	
	if (debugOutput) runIn(300,debugOff) //disable debug logs after 5 min
	initialize()
}

def initialize() 
{
	logDebug('initialize()')

	if (!state.deviceCache) {
		state.deviceCache = [
			'groups': [:],
			'devices': [:],
			'groupDeviceMacs': []
		]
	}

	if (!state.deviceParentMap) {
		state.deviceParentMap = [:]
	}

	if(settings['addDevices']) {
		logDebug('clearing addDevices cache')
		app.removeSetting('addDevices')
	}

	if (logEnable) {
		logInfo('Logging is Enabled')
		if (debugEnabled) {
			logDebug('Debug Logging is Enabled')
		}
	}

}

def uninstalled() 
{
	logDebug('uninstalled()')
	
	logInfo("Uninstalling...")

	logInfo('Clearing State...')
	clearState()

	logInfo("Deleting child devices...")
	getChildDevices().each { device->
		logDebug("Deleting device: " + device.deviceNetworkId)
		deleteChildDevice(device.deviceNetworkId)
	}

	logInfo("Uninstalled")
}

//  -------
// | Pages |
//  -------

def getCopyright() {'&copy; 2021 Your Mom'}

def pageMenu() 
{
	logDebug('pageMenu()')
	
	if (settings["devicesToAdd"]) {
		logDebug("New devices selected. Creating...")
		addDevices(settings["devicesToAdd"])
		app.removeSetting('devicesToAdd')
	}

    if (settings["deviceGroupsToAdd"]) {
		logDebug("New groups selected. Creating...")
        addDeviceGroups(settings["deviceGroupsToAdd"])
		app.removeSetting('deviceGroupsToAdd')
	}
	
	if (!(settings.username && settings.password)) {
		logDebug('Auth Credentials not set. Forwarding to pageAuthSettings...')
		return pageAuthSettings()
	}

	initialize()

	return dynamicPage(
		name: 'pageMenu', 
		title: "${app.label}", 
		install: true, 
		uninstall: true, 
		refreshInterval: 0
	) {

		section() {
			href(name: 'hrefSelectDeviceGroups', title: 'Select Device Groups',
               description: '', page: 'pageSelectDeviceGroups', image: '')
			href(name: 'hrefSelectDevices', title: 'Select Devices',
               description: '', page: 'pageSelectDevices', image: '')
        	href(name: 'hrefAuthSettings', title: 'Configure Login Info',
				description: '', page: 'pageAuthSettings')
      	}

		section('App Options') {
			logLevelOptions = [
				'5': 'Debug',
				'4': 'Info',
				'2': 'Warn',
				'1': 'Error',
				'0': 'Off',
			]
			input name: "logLevel", type: "enum", title: "Log Level", required: true, defaultValue: '4', submitOnChange: true, options: logLevelOptions

		}       
      	displayFooter()
	}	
}

def pageAuthSettings() {
	logDebug('pageAuthSettings()')
	
	return dynamicPage(
		name: 'pageAuthSettings', 
		title: "${app.label} Account Info", 
		install: false, 
		uninstall: false, 
		refreshInterval: 0,
		nextPage: 'pageDoAuth'
	) {
		section() {
            input name: 'username', type: 'text', title: 'Username', required: true, submitOnChange: true
            input name: 'password', type: 'password', title: 'Password', required: true, submitOnChange: true
        }
   		displayFooter()
	}
}

def pageDoAuth() {
	logDebug('pageDoAuth()')

	if (!(settings.username && settings.password)) {
		logError('Made it to "Do Auth" but credentials not set? Forwarding to pageAuthSettings...')
		return pageAuthSettings()
	}

	nextPage = 'pageMenu'
	loggedIn = false
	mfaEnabled = false
	clearState()
	authenticateWyzeAccount(settings.username, settings.password)

	if (state.access_token) {
		logDebug('access token found')
		logDebug(state.access_token)
		loggedIn = true
	} else if (state.mfa_options) {
		
		mfaEnabled = true
		nextPage = 'pageDoMfaAuth'

		if (state.mfa_options.contains('TotpVerificationCode')) {
			// TOTP
			logInfo('TOTP MFA Enabled')
			mfaTitle = 'TOTP MFA Enabled'
			mfaText = 'Enter TOTP Code'
			state.mfa_type = 'TotpVerificationCode'
		} else if(state.mfa_options.contains('PrimaryPhone')) {
			// SMS
			logInfo('SMS MFA Enabled')
			mfaTitle = 'SMS MFA Enabled'
			mfaText = 'Enter SMS Code'
			state.mfa_type = 'PrimaryPhone'
			sendSmsCode('Primary', state.sms_session_id, state.user_id)
		} else {
			logError('No supported MFA Types Found')
			logDebug(state.mfa_options)
		}		
	}

	return dynamicPage(
		name: 'pageAuthSettings', 
		title: "${app.label} Authentication", 
		install: false, 
		uninstall: false, 
		refreshInterval: 0,
		nextPage: nextPage
	) {
		
		if (loggedIn) {
			section() {
				paragraph("Logged In!")
			}
		} else if (mfaEnabled) {
			mfaCode = null
			settings.mfaCode = null
			section('MFA Enabled') {
				input name: 'mfaCode', type: 'password', title: mfaText, defaultValue: '', required: true, submitOnChange: false
			}
		}

   		displayFooter()
	}
}

def pageDoMfaAuth() {
	logDebug('pageDoMfaAuth()')

	if (state.mfa_type == 'PrimaryPhone') {
		verificationId = state.sms_session_id
		verificationCode = settings.mfaCode
	} else if (state.mfa_type == 'TotpVerificationCode') { 
		verificationId = state.mfa_details.totp_apps[0]['app_id']
		verificationCode = settings.mfaCode
	} else {
		return 'pageAuthSettings'
	}

	loggedIn = false
	authenticateWyzeAccount(
		settings.username, 
		settings.password, 
		state.mfa_type, 
		verificationId, 
		verificationCode
	)
	
	if (state.access_token) {
		logDebug('access token found')
		loggedIn = true
	} else {
		logError('MFA Login Error')
	}

	return dynamicPage(
		name: 'pageAuthSettings', 
		title: "${app.label} Authentication", 
		install: false, 
		uninstall: false, 
		refreshInterval: 0,
		nextPage: 'pageMenu'
	) {
		
		if (loggedIn) {
			section() {
				paragraph("Logged In!")
			}
		} else {
			section() {
				"Failed logging in with MFA."
			}
		}

   		displayFooter()
	}

}

def pageSelectDeviceGroups() {
    logDebug('pageSelectDeviceGroups()')
	
	updateDeviceCache() { response ->
		deviceCache = response
	}
		
	List newGroups = []
	List unsupportedGroups = []
	
    if (deviceCache.groups) {
		deviceCache.groups.each { mac, group ->
			groupType = groupDriverMap[group.group_type_id]
			networkId = generateGroupNetworkId(group)

			if (getChildDevice(networkId)) {
				logDebug("${group.group_name} (${networkId}) already exists. Skipping...")
				return
			}

			if(!groupType) {
				logDebug("${group.group_name} (${networkId}) unsupported. Skipping...")
				unsupportedGroups << group
				return
			} 
			
			logDebug("${group.group_name} (${networkId}) found. Adding to selection list...")
			
			newGroups << [(group.group_id): "[${groupType.label}] ${group.group_name}"]
		}

		// Sort
		newGroups = newGroups.sort { a, b ->
			a.entrySet().iterator().next()?.value <=> b.entrySet().iterator().next()?.value
		}
		unsupportedGroups = unsupportedGroups.sort { it.value }

	}

	return dynamicPage(
		name: 'pageSelectDeviceGroups',
		title: "${app.label} Device Group Selection",
		install: false,
		uninstall: false,
		refreshInterval: 0,
		nextPage: 'pageMenu'
	) {
        
        section() {
            paragraph("This page will list any <strong>new</strong> supported device groups.")
        }
		
		if (!newGroups) {
			section("No New Device Groups Found...") {
				input(name: "btnDeviceRefresh", type: "button", title: "Refresh", submitOnChange: true)
			}
		} else {
			section('Add Device Groups') {
				input(name: "deviceGroupsToAdd", type: "enum", title: "Select Device Groups to add:",
					submitOnChange: false, multiple: true, options: newGroups)
			}
		}

		if(unsupportedGroups) {
			section('Unsupported Device Groups') {
				unsupportedGroups.each{ group ->
					paragraph " - [${group.group_type_id}] ${group.group_name}"
				}
			}
		}

   		displayFooter()
	}	
}

def pageSelectDevices() {
	logDebug('pageSelectDevices()')
	
	updateDeviceCache() { response ->
		deviceCache = response
	}
		
	List newDevices = []
	List unsupportedDevices = []
	
	if (deviceCache.devices) {
		deviceCache.devices.each { mac, device ->
			if (ignoreDeviceModels.contains(device.product_model)) {
				logDebug("${device.nickname} (${device.mac}) is ignored device model. Skipping...")
				return
			}

			productType = driverMap[device.product_type]
			
			if (getChildDevice(device.mac)) {
				logDebug("${device.nickname} (${device.mac}) already exists. Skipping...")
				return
			}

			if (deviceCache.groupDeviceMacs.contains(device.mac)) {
				logDebug("${device.nickname} (${device.mac}) belongs to a group. Skipping...")
				return
			}
			
			if(!productType) {
				logDebug("${device.nickname} (${device.mac}) unsupported. Model: ${device.product_model}. Type: ${device.product_type}. Skipping...")
				unsupportedDevices << device
				return
			} 
			
			logDebug("${device.nickname} (${device.mac}) found. Adding to selection list...")
			
			newDevices << [(device.mac): "[${productType.label}] ${device.nickname}"]
		}

		// Sort
		newDevices = newDevices.sort { a, b ->
			a.entrySet().iterator().next()?.value <=> b.entrySet().iterator().next()?.value
		}
		unsupportedDevices = unsupportedDevices.sort { it.value }

	}

	return dynamicPage(
		name: 'pageSelectDevices',
		title: "${app.label} Device Selection",
		install: false,
		uninstall: false,
		refreshInterval: 0,
		nextPage: 'pageMenu'
	) {

        section() {
            paragraph('This page will list any <strong>new</strong> devices that <strong>do not belong to a device group</strong>.')
        }
		
		if (!newDevices) {
			section("No New Devices Found...") {
				input(name: "btnDeviceRefresh", type: "button", title: "Refresh", submitOnChange: true)
			}
		} else {
			section('Add Devices') {
				input(name: "devicesToAdd", type: "enum", title: "Select Devices to add:",
					submitOnChange: false, multiple: true, options: newDevices)
			}
		}

		if(unsupportedDevices) {
			section('Unsupported Devices') {
				unsupportedDevices.each{ device ->
					paragraph " - [${device.product_type}] ${device.nickname}"
				}
			}
		}

   		displayFooter()
	}	
}

def displayFooter() {
	section{
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: Jake<br/>Current Version: ${version()} -  ${getCopyright()}</div>"
    }
}

def getFormat(type){
	if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
}

//  --------------
// | Biznas Logic |
//  --------------

def authenticateWyzeAccount(String username, String password, String mfaType = null, String verificationId = null, String verificationCode = null) {
    logInfo('Authenticating User...')

	body = [
		'email': username,
		'password': hashPassword(password)
	]

	if (mfaType) {
		body['mfa_type'] = mfaType
		body['verification_id'] = verificationId
		body['verification_code'] = verificationCode
	}

	logDebug(body)

    params = [
		'uri'                	: wyzeAuthBaseUrl(),
		'headers'            	: wyzeRequestHeaders(),
		'requestContentType' 	: "application/json; charset=utf-8",
		'path'			     	: "/user/login",
		'body' 					: body
	]

	try {
		httpPost(params) { response ->
			logInfo("Login Request was OK: ${response.status}")
			logDebug(response.data)
			state.access_token   = response.data?.access_token
			state.refresh_token  = response.data?.refresh_token
			state.user_id        = response.data?.user_id
			state.mfa_options 	 = response.data?.mfa_options
			state.mfa_details 	 = response.data?.mfa_details
			state.sms_session_id = response.data?.sms_session_id
			state.statusText	 = "Success"
		}
	} catch (Exception e) {
		logError("Login Failed with Exception: ${e}")
		clearState()
		state.statusText = "Login Exception: '${e}'"
		return false
	}

		return true
}

private def sendSmsCode(String mfaPhoneType, String smsSessionId, String userId) {
	logInfo('Sending SMS Code to ' + mfaPhoneType)

	params = [
		'uri'                : wyzeAuthBaseUrl(),
		'headers'            : wyzeRequestHeaders(),
		'requestContentType' : "application/json; charset=utf-8",
		'path'			     : "/user/login/sendSmsCode",
		'query' : [
			'mfaPhoneType': mfaPhoneType,
			'sessionId': smsSessionId,
			'userId': userId,
		]
	]

	try {
		httpPost(params) { response ->
			logDebug("Send SMS was OK: ${response.status}")
			state.sms_session_id = "${response.data?.session_id}"
		}
	} catch (Exception e) {
		logError("Send SMS Failed with Exception: ${e}")
		clearState()
		state.statusText = "Send SMS Exception: '${e}'"
		return false
	}

		return true
}

private def updateDeviceCache(Closure closure = null) {
	logDebug("updateDeviceCache()")
	state.deviceCache = [
		'groups': [:],
		'devices': [:],
		'groupDeviceMacs': []
	]
	requestBody = wyzeRequestBody() + ['sv': 'c417b62d72ee44bf933054bdca183e77']
	apiPost('/app/v2/home_page/get_object_list', requestBody) { response ->

		response.data.device_group_list.each { deviceGroup ->
			state.deviceCache['groups'][deviceGroup.group_id] = deviceGroup
			deviceGroup.device_list.each {
				state.deviceCache['groupDeviceMacs'] << it.device_mac
			}
		}

		response.data.device_list.each { device ->
			state.deviceCache['devices'][device.mac] = device
		}

		if(closure) {
			closure(state.deviceCache)
		}
	}
}

private def Map getDeviceCache() {
	return state.deviceCache
}

private def Map getDeviceFromCache(String mac) {
	return state.deviceCache['devices'][mac]
}

private def Map getDeviceGroupFromCache(String id) {
	return state.deviceCache['groups'][id]
}

private def Map getDeviceListFromCache() {
	return state.deviceCache['devices']
}

private def Map getDeviceGroupListFromCache() {
	return state.deviceCache['groups']
}

private def addDeviceGroups(List deviceGroupIds) {
	logDebug('addDeviceGroups()')

	deviceGroupIds.each { deviceGroupId ->
		Map deviceGroupFromCache = getDeviceGroupFromCache(deviceGroupId)
        
		if (deviceGroupFromCache) {
			logInfo("Adding device group ${deviceGroupFromCache.group_name} with id ${deviceGroupFromCache.id}")
		
			driver = groupDriverMap[deviceGroupFromCache.group_type_id].driver
			if (!driver) {
				logError("Driver not found. Unsupported Device Group Type: ${deviceGroupFromCache.group_type_id}")
				return
			}

            groupNetworkId = generateGroupNetworkId(deviceGroupFromCache)
			deviceProps = [
				name: driver, 
				label: deviceGroupFromCache.group_name,
			]
			groupDevice = addChildDevice(childNamespace, driver, groupNetworkId, deviceProps)
            
            // Add Child Devices
            deviceGroupFromCache.device_list.each { device ->
                mac = device.device_mac
                Map deviceFromCache = getDeviceFromCache(mac)
                if (deviceFromCache) {
                    logInfo("Adding device group child device type ${deviceFromCache.product_type} with mac ${mac}")
                
                    driver = driverMap[deviceFromCache.product_type].driver
                    if (!driver) {
                        logError("Driver not found. Unsupported Device Type: ${deviceFromCache.product_type}")
                        return
                    }
					
                    deviceProps = [
                        name: (driver), 
                        label: (deviceFromCache.nickname),
                        deviceModel: (deviceFromCache.product_model)
                    ]

					state.deviceParentMap[mac] = groupNetworkId
                    device = groupDevice.addChildDevice(childNamespace, driver, deviceFromCache.mac, deviceProps)

					deviceFromCache.each { key, value ->
						if (!(key && value)) {
							logInfo('key or value not set')
							return
						}
						logInfo("setting ${key} to ${value}")
						device.updateDataValue(key, value.toString())
					}
                }
            }
		}
	}
}

private def addDevices(List deviceMacs) {
	logDebug('addDevices()')

	deviceMacs.each { mac ->
		Map deviceFromCache = getDeviceFromCache(mac)
		if (deviceFromCache) {
			logInfo("Adding device type ${deviceFromCache.product_type} with mac ${mac}")
		
			driver = driverMap[deviceFromCache.product_type].driver
			if (!driver) {
				logError("Driver not found. Unsupported Device Type: ${deviceFromCache.product_type}")
				return
			}
			deviceProps = [
				name: (driver), 
				label: (deviceFromCache.nickname),
				deviceModel: (deviceFromCache.product_model)
			]
			device = addChildDevice(childNamespace, driver, deviceFromCache.mac, deviceProps)

			deviceFromCache.each { key, value ->
				if (!(key && value)) {
					return
				}
				device.updateDataValue(key, value.toString())
			}
		}
	}
}

private def doSendDeviceEvent(com.hubitat.app.DeviceWrapper device, eventName, eventValue, eventUnit) {
	logDebug("doSendDeviceEvent()")

	String descriptionText = "${device.displayName} ${eventName} is ${eventValue}${eventUnit ?: ''}"

    eventData = [
		'name': eventName,
		'value': eventValue,
        'unit': eventUnit,
		'description': descriptionText,
		'isStateChange': true
	]

	if (eventUnit) {
		properties['eventUnit'] = eventUnit
	}

	logDebug('Sending event data...')
	logDebug(eventData)

	sendEvent(device, eventData)
}

def apiGetDevicePropertyList(String deviceMac, String deviceModel, Closure closure = {}) {
	logDebug("apiGetDevicePropertyList()")
	
	requestBody = wyzeRequestBody() + [
		'sv': 'c417b62d72ee44bf933054bdca183e77',
		'device_mac': deviceMac,
    	'device_model': deviceModel
	]

	callbackData = [
		'deviceNetworkId': deviceMac
	]

	asyncapiPost('/app/v2/device/get_property_list', requestBody, 'deviceEventsCallback', callbackData)

}

def apiRunAction(String deviceMac, String deviceModel, String actionKey, Closure closure = {}) {
	logDebug("apiRunAction()")
	
	requestBody = wyzeRequestBody() + [
		'sv': '011a6b42d80a4f32b4cc24bb721c9c96', 
		'action_key': actionKey,
		'action_params': [:],
		'instance_id': deviceMac,
		'provider_key': deviceModel
	]

	callbackData = [
		'deviceNetworkId': deviceMac,
		'propertyList': [
			[
				'pid': actionKey,
				'pvalue': actionKey
			]
		]
	]

	asyncapiPost('/app/v2/auto/run_action', requestBody, 'deviceEventsCallback', callbackData)
	
}

def apiRunActionList(String deviceMac, String deviceModel, List actionList) {
	logDebug("apiRunActionList()")
	logDebug(['mac': deviceMac, 'model': deviceModel, 'actionList': actionList])

	List apiActionList = [
		[
			'action_key': 'set_mesh_property',
			'instance_id': deviceMac,
			'provider_key': deviceModel,
			'action_params': [
				'list': [
						[
							'mac': deviceMac,
							'plist': actionList
						]
					]
			]
		]
	]

	requestBody = wyzeRequestBody() + [
		'sv': '5e02224ae0c64d328154737602d28833', 
		'action_list': apiActionList
	]

	callbackData = [
		'deviceNetworkId': deviceMac,
		'propertyList': actionList
	]

	asyncapiPost('/app/v2/auto/run_action_list', requestBody, 'deviceEventsCallback', callbackData)
}

def apiSetDeviceProperty(String deviceMac, String deviceModel, String propertyId, value) {
	logDebug("setDeviceProperty()")
	logDebug(['mac': deviceMac, 'model': deviceModel, 'propertyId': propertyId, 'value': value])

	requestBody = wyzeRequestBody() + [
		'sv': '44b6d5640c4d4978baba65c8ab9a6d6e',
		'device_mac': deviceMac,
		'device_model': deviceModel, 
		'pid': propertyId,
		'pvalue': value
	]

	callbackData = [
		'deviceNetworkId': deviceMac,
		'propertyList': [
			[
				'pid': propertyId,
				'pvalue': value
			]
		]
	]
	
	asyncapiPost('/app/v2/device/set_property', requestBody, 'deviceEventsCallback', callbackData)
}

def asyncapiPost(String path, Map body = [:], String callbackMethod = null, Map callbackData = [:]) {
	logDebug('asyncapiPost()')

	bodyJson = (new JsonBuilder(body)).toString()

    params = [
		'uri'         : wyzeApiBaseUrl(),
		'headers'     : wyzeRequestHeaders(),
		'contentType' : 'application/json',
		'path'        : path,
		'body'        : bodyJson
	]

	asynchttpPost(callbackMethod, params, callbackData) 
}

def apiPost(String path, Map body = [], Closure closure = {}) {
	logDebug('apiPost()')

	if (!(body.access_token || body.refresh_token)) {
		throw new Exception('No Auth Tokens. Reauthenticate.')
	}

	bodyJson = (new JsonBuilder(body)).toString()

    params = [
		'uri'         : wyzeApiBaseUrl(),
		'headers'     : wyzeRequestHeaders(),
		'contentType' : 'application/json',
		'path'        : path,
		'body'        : bodyJson
	]

	try {
		httpPost(params) { response -> 
			validateApiResponse(response)
			closure(response.data) 
		}
	} catch (Exception e) {
		logError("API Call to ${params.uri}${params.path} failed with Exception: ${e}")
	}
}

private validateApiResponse(response) {
	logDebug("validateApiResponse()")
	
	if (response.hasProperty('message')) {
		// i.e. 'Rate limit is exceeded.'
		// TODO do something
		logError(response.message)
		throw new Exception(response.message)
	}
	
	if (response.data instanceof String) {
		responseData = parseJson(response.data)
	} else {
		responseData = response.data
	}

	if (responseData.code == "2001") {
		logError("Access Token Invalid. Attempting to refresh token.")
		logDebug(response.data)
		refreshAccessToken() { refreshTokenResponse ->
			return false
		}
	}

	if (responseData.code == "2002") {
		// Refresh Token Error
		logError("Refresh Token Invalid.")
		clearState()
		throw new Exception("Refresh Token Invalid")
	}

	if (responseData.code != "1") {
		logError("API Response error!")
		logDebug(response.data)
		throw new Exception("Invalid Response Data Code: ${response.data.code}")
	}

	return true
}

private refreshAccessToken(Closure closure = {}) {
	logDebug('refreshAccessToken()')

	requestBody = wyzeRequestBody() + [
		'sv': 'd91914dd28b7492ab9dd17f7707d35a3',
		'refresh_token': state.refresh_token
	]
	requestBody['access_token'] = null

	apiPost('/app/user/refresh_token', requestBody) { response ->
		logDebug("refreshToken Resposne:")
		logDebug(response.data)
		state.access_token = response.data.access_token
		state.refresh_token = response.data.refresh_token
		closure(response)
	}
}

private void deviceEventsCallback(response, data) {
	logDebug("deviceEventsCallback() for device ${data.deviceNetworkId}")

	validateApiResponse(response)

	if (!response.data) {
		logError("No response data sent to deviceEventsCallback()")
		return;
	}

	responseData = parseJson(response.data)

	propertyList = data.propertyList ?: responseData.data.property_list ?: []
	
	if (!(data.deviceNetworkId && propertyList)) {
		logDebug('Missing deviceNetworkId or propertyList')
		return
	}

	parentNetworkId = state.deviceParentMap[data.deviceNetworkId]
	if (parentNetworkId) {
		device = getChildDevice(parentNetworkId).getChildDevice(data.deviceNetworkId)
	} else {
		device = getChildDevice(data.deviceNetworkId)
	}

	if (!device) {
		logDebug("Device ${data.deviceNetworkId} not found")
		return
	}

	device.createDeviceEventsFromPropertyList(propertyList)
}

private String getPhoneId() {
	if (!state.phone_id) {
		state.phone_id = randomUUID() as String
	}
	return state.phone_id
}

private String generateGroupNetworkId(Map wyzeGroupDetails) {
    return wyzeGroupDetails.group_type_id + '.' + wyzeGroupDetails.group_id
}

private String hashPassword(String password) {
	return md5(md5(md5(password)))
}

private String md5(String str) {
	return MessageDigest.getInstance("MD5").digest(str.bytes).encodeHex().toString()
}

private clearState() {
	state.access_token   = null
	state.refresh_token  = null
	state.user_id        = null
	state.mfa_options 	 = null
	state.mfa_details 	 = null
	state.sms_session_id = null
	state.statusText     = null
}

void appButtonHandler(btn) {
   switch(btn) {
      case "btnDeviceRefresh":
         // Just want to resubmit page, so nothing
         break        
      default:
         log.warn "Unhandled app button press: $btn"
   }
}

// def debugOff()
// {
// 	logWarn("Debug logging disabled...")
// 	app?.updateSetting("debugEnabled",[value:"false",type:"bool"])
// }

private void logDebug(message) {
	level = settings.logLevel ?: log_level_default
	if (level >= log_level_debug) {
		log.debug("[${app.label}] " + message)
	}
}

private void logInfo(message) {
	level = settings.logLevel ?: log_level_default
	if (level >= log_level_info) {
		log.info("[${app.label}] " + message)
	}
}

private void logWarn(message) {
	level = settings.logLevel ?: log_level_default
	if (level >= log_level_warn) {
		log.warn("[${app.label}] " + message)
	}
}

private void logError(message) {
	level = settings.logLevel ?: log_level_default
	if (level >= log_level_error) {
		log.error("[${app.label}] " + message)
	}
}
