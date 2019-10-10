import flask
import mysql.connector
import sys
import smtplib, ssl
from email.mime.text import MIMEText
import cryptography.fernet

app = flask.Flask(__name__)

@app.route('/', methods = ['GET', 'POST'])
def chat():
    msg_received = flask.request.get_json()
    msg_subject = msg_received["subject"]

    if msg_subject == "register":
        return register(msg_received)
    elif msg_subject == "login":
        return login(msg_received)
    elif msg_subject == "verify":
        return verify(msg_received)
    else:
        return "Invalid request."

def register(msg_received):
    first_name = msg_received["first_name"]
    lastname = msg_received["last_name"]
    username = msg_received["username"]
    email = msg_received["email"]

    select_query = "SELECT * FROM users where username = " + "'" + username + "'"
    db_cursor.execute(select_query)
    records = db_cursor.fetchall()
    if len(records) != 0:
        return "username" # "Username already exists. Please chose another username."

    select_query = "SELECT * FROM users where email = " + "'" + email + "'"
    db_cursor.execute(select_query)
    records = db_cursor.fetchall()
    if len(records) != 0:
        return "email" # "E-mail is already registered."

    send_verification_code(first_name, lastname, username, email) # Encrypt e-mail using 2 keys and send the code to the user e-mail.

    return "success"

def verify(msg_received):
    firstname = msg_received["firstname"]
    lastname = msg_received["lastname"]
    username = msg_received["username"]
    email = msg_received["email"]
    password = msg_received["password"]
    verification_code = msg_received["verification_code"]

    select_query = "SELECT * FROM users where username = " + "'" + username + "'"
    db_cursor.execute(select_query)
    records = db_cursor.fetchall()
    if len(records) != 0:
        return "Another user just used the username. Please chose another username."

    select_query = "SELECT * FROM users where email = " + "'" + email + "'"
    db_cursor.execute(select_query)
    records = db_cursor.fetchall()
    if len(records) != 0:
        return "Another user just registered using the e-mail entered."

    verification_state = verify_code(email, verification_code)

    if verification_state == True:
        insert_query = "INSERT INTO users (first_name, last_name, username, email, password, email_confirmed) VALUES (%s, %s, %s, %s, MD5(%s), %s)"
        insert_values = (firstname, lastname, username, email, password, True)
        try:
            db_cursor.execute(insert_query, insert_values)
            chat_db.commit()
            print("This e-mail", email, " is verified and user is registered successfully.")
            subject = "Welcome to HiAi"
            body = "Hi " + firstname + " " + lastname + ",\n\nYour account is created successfully at HiAi. You can login with your username and password into the Android app to send and receive messages. \n\nYour username is " + username + "\n\nIf you forgot your password, just contact ahmedgad@hiai.website for resetting it. \n\nGood luck.\nHiAi Team\nahmedgad@hiai.website"
            send_email(subject, body, email)
            return "success"
        except Exception as e:
            print("Error while inserting the new record :", repr(e))
            return "Error while processing the request."
    else:
        print("This e-mail", email, " verification failed.")
        return "failure"

def verify_code(email, verification_code):
    decoded_email = decrypt_text(verification_code)

    if decoded_email == email:
        return True # Email verification succeeded.
    else:
        return False # Email verification failed.

def encrypt_text(text_to_encrypt):
    key_file1 = open("encryption_key1.key", "rb")
    encryption_key1 = key_file1.read()
    key_file1.close()
    
    key_file2 = open("encryption_key2.key", "rb")
    encryption_key2 = key_file2.read()
    key_file2.close()

    encoded_email = text_to_encrypt.encode()

    f = cryptography.fernet.MultiFernet([cryptography.fernet.Fernet(encryption_key1), cryptography.fernet.Fernet(encryption_key2)])
    
    encrypted_text = f.encrypt(encoded_email)
    
    return encrypted_text

def decrypt_text(text_to_decrypt):
    key_file1 = open("encryption_key1.key", "rb")
    encryption_key1 = key_file1.read()
    key_file1.close()
    
    key_file2 = open("encryption_key2.key", "rb")
    encryption_key2 = key_file2.read()
    key_file2.close()
    
    f = cryptography.fernet.MultiFernet([cryptography.fernet.Fernet(encryption_key1), cryptography.fernet.Fernet(encryption_key2)])

    try:
        decrypted_text = f.decrypt(text_to_decrypt.encode('utf-8'))
    except:
        return False # Email verification failed.

    decoded_text = decrypted_text.decode()

    return decoded_text

def send_verification_code(firstname, lastname, username, email):
    
    encrypted_email = encrypt_text(email)
    
    body = "Hi " + firstname + " " + lastname + ",\n\nThanks for registering for HiAi Chat System.\n\nYour username is " + username + ".\nTo verify your account, just copy the verification code found below, return back to the Android app, paste the code, and finally click the Verify button.\n\n\n" + encrypted_email.decode()
    subject = "HiAi Verification"

    send_email(subject, body, email)

def login(msg_received):
    username = msg_received["username"]
    password = msg_received["password"]

    select_query = "SELECT first_name, last_name FROM users where username = " + "'" + username + "' and password = " + "MD5('" + password + "')"
    db_cursor.execute(select_query)
    records = db_cursor.fetchall()

    if len(records) == 0:
        return "failure"
    else:
        return "success"

def send_email(subject, body, email):
    port = 465  # For SSL
#    smtp_server = "smtp.gmail.com"
    smtp_server = "hiai.website"
    sender_email = "ahmedgad@hiai.website"  # Enter your address
#    sender_email = "afgfcit@gmail.com"  # Enter your address
    password = "..."

    msg_body = body
    
    msg = MIMEText(msg_body, 'plain', 'utf-8')

    # add in the actual person name to the message template
    msg['From'] = sender_email
    msg['To'] = email
    msg['Subject'] = subject

    context = ssl.create_default_context()
    with smtplib.SMTP_SSL(smtp_server, port, context=context) as server:
        server.login(sender_email, password)
        server.sendmail(sender_email, email, msg.as_string())

try:
    chat_db = mysql.connector.connect(host="localhost", user="root", passwd="ahmedgad", database="chat_db")
except:
    sys.exit("Error connecting to the database. Please check your inputs.")

db_cursor = chat_db.cursor()

app.run(host="0.0.0.0", port=5000, debug=True, threaded=True) # Run in development server.
# waitress.serve(app=app, host="0.0.0.0", port=5000) # Run in production server.