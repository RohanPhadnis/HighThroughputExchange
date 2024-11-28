import json
import urllib.request

URL = 'http://localhost:8080'



# first, an admin will create a user
form_data = {
	'adminUsername': 'trading_club_admin',
	'adminPassword': 'abcxyz',
	'username': 'team1',
	'name': 'National Fellas League',
	'email': 'team_email@email.com'
}
req = urllib.request.Request(URL + '/add_user', data=json.dumps(form_data).encode('utf-8'), method='POST')
req.add_header('Content-Type', 'application/json')
resp = json.loads(urllib.request.urlopen(req).read().decode('utf-8'))
print(resp)




# then, user will buildup to get an active session
# this buildup includes a session token which the user can authenticate with
# the user also uses the session token for subscribing to orderbook socket
form_data = {
	'username': 'team1',
	'apiKey': resp['apiKey']
}
req = urllib.request.Request(URL + '/buildup', data=json.dumps(form_data).encode('utf-8'), method='POST')
req.add_header('Content-Type', 'application/json')
resp = json.loads(urllib.request.urlopen(req).read().decode('utf-8'))
print(resp)
session_token = resp['sessionToken']




# user can now make trades
form_data = {
	'username': 'team1',
	'sessionToken': session_token,
	'ticker': 'AAPL',
	'volume': 69,
	'price': 420,
	'isBid': True
}
req = urllib.request.Request(URL + '/limit_order', data=json.dumps(form_data).encode('utf-8'), method='POST')
req.add_header('Content-Type', 'application/json')
resp = json.loads(urllib.request.urlopen(req).read().decode('utf-8'))
print(resp)




# once the user is done, they teardown to delete their session token
form_data = {
	'username': 'team1',
	'sessionToken': session_token
}
req = urllib.request.Request(URL + '/teardown', data=json.dumps(form_data).encode('utf-8'), method='POST')
req.add_header('Content-Type', 'application/json')
resp = json.loads(urllib.request.urlopen(req).read().decode('utf-8'))
print(resp)




# finally, the admin shuts off the server to save all RAM into storage as a long-lived database
form_data = {
	'adminUsername': 'trading_club_admin',
	'adminPassword': 'abcxyz'
}
req = urllib.request.Request(URL + '/shutdown', data=json.dumps(form_data).encode('utf-8'), method='POST')
req.add_header('Content-Type', 'application/json')
resp = json.loads(urllib.request.urlopen(req).read().decode('utf-8'))
print(resp)


