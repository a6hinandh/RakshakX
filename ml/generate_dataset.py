"""
generate_dataset.py
Generates synthetic labeled scam datasets for DistilBERT and IndicBERT training.
Labels: 0=SAFE, 1=SCAM, 2=SUSPICIOUS
"""
import pandas as pd
import random
import os

random.seed(42)

# ─── English / Hinglish dataset (for DistilBERT) ──────────────────────────────

ENGLISH_SAFE = [
    "Your OTP is 847291. Valid for 10 minutes. Do not share with anyone.",
    "Your order #78432 has been shipped. Expected delivery by tomorrow.",
    "Hi, are you free for lunch today at 1pm?",
    "Transaction of Rs.500 at Swiggy on your card ending 4321. Not you? Call 1800.",
    "Your account balance is Rs.12,450. Last updated: today.",
    "Reminder: Your appointment is scheduled for tomorrow at 10 AM.",
    "Your package will be delivered between 2-4 PM today.",
    "Salary credited: Rs.45,000 on 01-May. Available balance: Rs.52,300.",
    "Your electricity bill of Rs.1,200 is due on 15th May.",
    "Thanks for shopping at Amazon. Your return has been processed.",
    "Good morning! Hope you have a great day.",
    "Meeting rescheduled to 3pm in conference room B.",
    "Your flight PNR is ABC123. Check-in opens 24 hours before departure.",
    "Your Aadhaar update request is being processed.",
    "Netflix: Your subscription renews on May 15 for Rs.499.",
    "Bhai kal milenge kya? Game khelne jaana hai.",
    "Paytm: Rs.200 sent to Rahul successfully.",
    "Your UPI PIN has been changed successfully.",
    "IRCTC: Your ticket for 12345 is confirmed. PNR: 987654.",
    "Your data pack of 1.5GB/day has been activated.",
    "Please rate your recent experience with Zomato.",
    "Your gas cylinder booking is confirmed for May 10.",
    "Google Pay: Rs.1500 received from Priya.",
    "Your premium plan expires in 3 days. Renew to continue.",
    "Gym reminder: Your class starts at 6 AM tomorrow.",
    "Blood test report is ready. Please collect from lab.",
    "Happy Birthday! Wishing you a wonderful year ahead.",
    "Your driving license renewal application has been submitted.",
    "EMI of Rs.3,200 debited successfully for your loan.",
    "Your PAN card has been dispatched. Tracking: XY123.",
]

ENGLISH_SCAM = [
    "CONGRATULATIONS! You won Rs.10,00,000 in KBC lottery. Call 9876543210 NOW!",
    "Your KYC is expired. Update immediately or account blocked: http://fake-bank.in/kyc",
    "Dear customer, your SBI account is suspended. Verify at: sbi-secure.xyz",
    "You have been selected for PM housing scheme. Pay Rs.5000 to register.",
    "URGENT: Income tax refund of Rs.15,890 pending. Claim at: incometax-refund.co",
    "Your Aadhaar is linked to criminal activity. Call cybercrime officer: 8765432109.",
    "FREE iPhone 15 for lucky BSNL users! Click: free-iphone-india.com",
    "Work from home earn Rs.50,000/month. No investment. WhatsApp: 9988776655",
    "Your EPF account has unclaimed Rs.78,000. Update KYC: epfo-claim.xyz",
    "AMAZON: You are selected for a survey. Get Rs.5000 gift card. Tap here.",
    "Loan approved Rs.2,00,000 at 0% interest. No documents needed. Call now.",
    "Dear user, your Gmail will be deleted in 24 hrs. Verify: google-secure.site",
    "Paytm: Your account flagged for suspicious activity. Login: paytm-verify.in",
    "FedEx: Package held at customs. Pay Rs.499 clearance fee: fedex-india.net",
    "You have won a free trip to Dubai! Register now at: dubai-tour-free.com",
    "HDFC: Unusual login detected. Reset password: hdfc-bank-secure.xyz",
    "Crypto investment: 300% guaranteed returns in 7 days. Join now!",
    "Aapka bijli connection katne wala hai. Abhi Rs.500 pay karo: bijli-pay.co",
    "Government free laptop scheme: Register at: laptop-scheme-2024.in",
    "Your WhatsApp account will expire. Pay Rs.100 to renew. Click here.",
    "Share this message with 10 friends and win Rs.10,000 instantly.",
    "URGENT: Police complaint filed against your number. Call officer: 7654321098",
    "Your bank account shows suspicious login from another country. Verify now.",
    "You are pre-approved for credit card. No income proof. Apply: card-now.in",
    "Mukesh Bhai mujhe paise chahiye aaj. Please 2000 bhejo. Emergency hai.",
    "RBI lottery draw: Your number WON! Claim within 24 hours.",
    "Trusted investment: Rs.1000 doubles in 5 days. Proof available.",
    "Job offer: Data entry work from home. Rs.800/hour. Register: jobs-india.co",
    "ICICI alert: Rs.49,999 about to be debited. Cancel at: icici-stop.xyz",
    "Your Jio SIM will be blocked in 2 hours. Update KYC: jio-kyc-update.in",
]

ENGLISH_SUSPICIOUS = [
    "Please send me Rs.2000 urgently. Something came up. Will explain later.",
    "Click this link to claim your delivery: http://track-pkg.cc/abc123",
    "Your account shows unusual activity. Please verify your details here.",
    "Congratulations, you may have won a prize. Check eligibility now.",
    "We need to verify your identity. Reply with your date of birth.",
    "Undelivered package in your name. Reschedule: delivery-now.site",
    "Hello, I am from your bank. Can you confirm your account number?",
    "Investment opportunity with high returns. Limited time offer.",
    "Aapko ek prize mila hai. Details ke liye is link pe click karo.",
    "Your application is under review. Share these documents urgently.",
    "Someone tried to access your account. Verify: secure-check.net",
    "Bhai ek kaam karo. Meri taraf se 1500 bhej do. Kal waapis karunga.",
    "Exciting job opportunity. Work from anywhere. Reply for details.",
    "You have an unclaimed package. Track here: pkg-track.online",
    "Your mobile number is eligible for a free recharge. Tap to claim.",
    "Warning: Your device may be compromised. Install protection now.",
    "Hi, remember me? I need a small favor urgently. Can you help?",
    "You are selected for a focus group. Get Rs.500 for 30 minutes.",
    "Please confirm your recent transaction of Rs.9,999 is authorized.",
    "Hello. I'm contacting you regarding an inheritance from your relative.",
]

# ─── Indic language dataset (for IndicBERT) ───────────────────────────────────

INDIC_SAFE = [
    # Hindi
    "आपका OTP 847291 है। 10 मिनट में समाप्त होगा। किसी के साथ साझा न करें।",
    "आपका ऑर्डर #78432 भेज दिया गया है। कल तक डिलीवरी होगी।",
    "आपके खाते में Rs.45,000 का वेतन जमा हो गया है।",
    "आपकी बिजली का बिल Rs.1,200 है जो 15 मई को देय है।",
    "नमस्ते! क्या आप कल मिल सकते हैं?",
    # Tamil
    "உங்கள் OTP 847291. 10 நிமிடங்களில் காலாவதியாகும். யாரிடமும் பகிர வேண்டாம்.",
    "உங்கள் ஆர்டர் #78432 அனுப்பப்பட்டது. நாளை டெலிவரி எதிர்பார்க்கப்படுகிறது.",
    "உங்கள் சம்பளம் Rs.45,000 வரவு வைக்கப்பட்டது.",
    "வணக்கம்! நாளை சந்திக்கலாமா?",
    # Telugu
    "మీ OTP 847291. 10 నిమిషాల్లో గడువు తీరుతుంది. ఎవరికీ చెప్పవద్దు.",
    "మీ ఆర్డర్ #78432 పంపబడింది. రేపు డెలివరీ అవుతుంది.",
    "మీ జీతం Rs.45,000 జమ అయింది.",
    # Kannada
    "ನಿಮ್ಮ OTP 847291 ಆಗಿದೆ. 10 ನಿಮಿಷಗಳಲ್ಲಿ ಅಮಾನ್ಯವಾಗುತ್ತದೆ. ಯಾರಿಗೂ ಹಂಚಿಕೊಳ್ಳಬೇಡಿ.",
    "ನಿಮ್ಮ ಆರ್ಡರ್ #78432 ಕಳುಹಿಸಲಾಗಿದೆ. ನಾಳೆ ಡೆಲಿವರಿ ಆಗುತ್ತದೆ.",
    # Malayalam
    "നിങ്ങളുടെ OTP 847291 ആണ്. 10 മിനിറ്റിൽ കാലഹരണപ്പെടും. ആരോടും പറയരുത്.",
    "നിങ്ങളുടെ ഓർഡർ #78432 അയച്ചു. നാളെ ഡെലിവറി ആകും.",
    # Bengali
    "আপনার OTP 847291। ১০ মিনিটে মেয়াদ শেষ হবে। কারো সাথে শেয়ার করবেন না।",
    "আপনার অর্ডার #78432 পাঠানো হয়েছে। আগামীকাল ডেলিভারি হবে।",
    # Gujarati
    "તમારો OTP 847291 છે. 10 મિનિટમાં સમાપ્ત થઈ જશે. કોઈ સાથે શેર ન કરો.",
    "તમારો ઓર્ડર #78432 મોકલવામાં આવ્યો છે. કાલે ડિલિવરી થશે.",
    # Punjabi
    "ਤੁਹਾਡਾ OTP 847291 ਹੈ। 10 ਮਿੰਟਾਂ ਵਿੱਚ ਮਿਆਦ ਪੁੱਗ ਜਾਵੇਗੀ। ਕਿਸੇ ਨਾਲ ਸਾਂਝਾ ਨਾ ਕਰੋ।",
    "ਤੁਹਾਡਾ ਆਰਡਰ #78432 ਭੇਜ ਦਿੱਤਾ ਗਿਆ ਹੈ। ਕੱਲ੍ਹ ਡਿਲੀਵਰੀ ਹੋਵੇਗੀ।",
    # More Hindi
    "आपका गैस सिलेंडर बुकिंग 10 मई के लिए कन्फर्म है।",
    "आपकी परीक्षा का परिणाम घोषित हो गया है। पोर्टल पर देखें।",
    "Google Pay: Rs.1500 प्रिया से प्राप्त हुआ।",
    "आपकी EMI Rs.3,200 सफलतापूर्वक डेबिट हो गई।",
    "आपकी ट्रेन का PNR 987654 कन्फर्म है।",
    "आपकी डेटा पैक 1.5GB/दिन सक्रिय हो गई है।",
]

INDIC_SCAM = [
    # Hindi
    "बधाई हो! आपने KBC लॉटरी में Rs.10,00,000 जीते हैं। अभी कॉल करें: 9876543210",
    "आपका SBI खाता बंद होने वाला है। KYC अपडेट करें: sbi-kyc-update.in",
    "सरकारी आवास योजना में चयन। Rs.5000 पंजीकरण शुल्क जमा करें अभी।",
    "आपके आधार कार्ड पर आपराधिक मामला दर्ज हुआ है। साइबर अफसर को कॉल करें।",
    "आयकर विभाग: Rs.15,890 रिफंड लंबित है। दावा करें: incometax-refund.co",
    "URGENT: आपका EPF खाता में Rs.78,000 लंबित है। KYC: epfo-claim.xyz",
    "मुफ्त लैपटॉप योजना: अभी रजिस्टर करें: laptop-yojana.in",
    # Tamil
    "வாழ்த்துகள்! நீங்கள் KBC லாட்டரியில் Rs.10,00,000 வென்றீர்கள். இப்போது அழைக்கவும்!",
    "உங்கள் SBI கணக்கு நிறுத்தப்படும். KYC புதுப்பிக்கவும்: sbi-kyc-tamil.in",
    "அரசு வீட்டுத் திட்டம்: Rs.5000 பதிவு கட்டணம் செலுத்தவும்.",
    "உங்கள் ஆதார் அட்டை குற்றவியல் நடவடிக்கையுடன் இணைக்கப்பட்டுள்ளது!",
    # Telugu
    "అభినందనలు! మీరు KBC లాటరీలో Rs.10,00,000 గెలుచుకున్నారు. ఇప్పుడే కాల్ చేయండి!",
    "మీ SBI ఖాతా నిలిపివేయబడుతుంది. KYC అప్‌డేట్ చేయండి: sbi-kyc.xyz",
    "ప్రభుత్వ ఉచిత పథకం: Rs.5000 నమోదు రుసుము చెల్లించండి.",
    # Kannada
    "ಅಭಿನಂದನೆಗಳು! ನೀವು KBC ಲಾಟರಿಯಲ್ಲಿ Rs.10,00,000 ಗೆದ್ದಿದ್ದೀರಿ. ಈಗಲೇ ಕರೆ ಮಾಡಿ!",
    "ನಿಮ್ಮ SBI ಖಾತೆ ನಿಲ್ಲಿಸಲಾಗುತ್ತದೆ. KYC ನವೀಕರಿಸಿ: sbi-kyc-update.in",
    # Malayalam
    "അഭിനന്ദനങ്ങൾ! നിങ്ങൾ KBC ലോട്ടറിയിൽ Rs.10,00,000 നേടി. ഇപ്പോൾ വിളിക്കൂ!",
    "നിങ്ങളുടെ SBI അക്കൗണ്ട് ബ്ലോക്ക് ആകും. KYC അപ്ഡേറ്റ് ചെയ്യൂ: sbi-verify.in",
    # Bengali
    "অভিনন্দন! আপনি KBC লটারিতে Rs.10,00,000 জিতেছেন। এখনই কল করুন!",
    "আপনার SBI অ্যাকাউন্ট বন্ধ হবে। KYC আপডেট করুন: sbi-kyc-bd.in",
    "সরকারি বাড়ি প্রকল্প: Rs.5000 নিবন্ধন ফি দিন।",
    # Gujarati
    "અભિનંદન! તમે KBC લોટરીમાં Rs.10,00,000 જીત્યા. હવે ફોન કરો!",
    "તમારો SBI ખાતો બ્લોક થઈ જશે. KYC અપડેટ કરો: sbi-kyc-gu.in",
    # Punjabi
    "ਵਧਾਈ ਹੋ! ਤੁਸੀਂ KBC ਲਾਟਰੀ ਵਿੱਚ Rs.10,00,000 ਜਿੱਤੇ ਹਨ। ਹੁਣੇ ਕਾਲ ਕਰੋ!",
    "ਤੁਹਾਡਾ SBI ਖਾਤਾ ਬਲੌਕ ਹੋ ਜਾਵੇਗਾ। KYC ਅਪਡੇਟ ਕਰੋ: sbi-kyc-pb.in",
    # More Hindi scam
    "क्रिप्टो निवेश: 7 दिनों में 300% गारंटीड रिटर्न। अभी जुड़ें!",
    "पुलिस शिकायत आपके नंबर के खिलाफ दर्ज हुई। अभी अफसर को कॉल करें।",
    "मुफ्त iPhone 15 पाएं! रजिस्टर करें: free-iphone.co.in",
]

INDIC_SUSPICIOUS = [
    # Hindi
    "कृपया अभी Rs.2000 भेजें। कुछ जरूरी काम आया है। बाद में समझाऊंगा।",
    "इस लिंक पर क्लिक करें और अपना इनाम पाएं। सीमित समय।",
    "आपके खाते में संदिग्ध गतिविधि दिखी है। अपना विवरण यहां सत्यापित करें।",
    "नमस्ते, मैं आपके बैंक से बात कर रहा हूं। क्या आप अपना खाता नंबर बता सकते हैं?",
    "आपके नाम पर एक पैकेज आया है जो डिलीवर नहीं हो सका।",
    # Tamil
    "தயவுசெய்து இப்போது Rs.2000 அனுப்புங்கள். அவசரமான வேலை வந்தது.",
    "இந்த லிங்கை கிளிக் செய்து உங்கள் பரிசை பெறுங்கள்.",
    "உங்கள் கணக்கில் சந்தேகமான செயல்பாடு கண்டறியப்பட்டது.",
    # Telugu
    "దయచేసి ఇప్పుడు Rs.2000 పంపండి. అత్యవసర పని వచ్చింది.",
    "ఈ లింక్ క్లిక్ చేసి మీ బహుమతి పొందండి.",
    "మీ ఖాతాలో అనుమానాస్పద కార్యకలాపాలు గుర్తించబడ్డాయి.",
    # Kannada
    "ದಯವಿಟ್ಟು ಈಗ Rs.2000 ಕಳುಹಿಸಿ. ತುರ್ತು ಕೆಲಸ ಬಂದಿದೆ.",
    "ಈ ಲಿಂಕ್ ಕ್ಲಿಕ್ ಮಾಡಿ ಮತ್ತು ನಿಮ್ಮ ಬಹುಮಾನ ಪಡೆಯಿರಿ.",
    # Malayalam
    "ദയവായി ഇപ്പോൾ Rs.2000 അയക്കൂ. അടിയന്തര കാര്യം വന്നു.",
    "ഈ ലിങ്കിൽ ക്ലിക്ക് ചെയ്ത് നിങ്ങളുടെ സമ്മാനം നേടൂ.",
    # Bengali
    "অনুগ্রহ করে এখন Rs.2000 পাঠান। জরুরি কাজ এসেছে।",
    "এই লিঙ্কে ক্লিক করুন এবং আপনার পুরস্কার পান।",
    # Gujarati
    "કૃપા કરીને અત્યારે Rs.2000 મોકલો. કંઈક જરૂરી આવ્યું છે.",
    "આ લિંક ક્લિક કરો અને તમારું ઇનામ મેળવો.",
    # Punjabi
    "ਕਿਰਪਾ ਕਰਕੇ ਹੁਣੇ Rs.2000 ਭੇਜੋ। ਕੋਈ ਜ਼ਰੂਰੀ ਕੰਮ ਆਇਆ ਹੈ।",
    "ਇਸ ਲਿੰਕ 'ਤੇ ਕਲਿੱਕ ਕਰੋ ਅਤੇ ਆਪਣਾ ਇਨਾਮ ਲਓ।",
    # More Hindi
    "निवेश का बेहतरीन मौका। ज्यादा रिटर्न, कम जोखिम। जानकारी के लिए जवाब दें।",
    "आपके नाम पर विरासत में संपत्ति है। विवरण के लिए संपर्क करें।",
    "हेलो, मुझे याद है? एक छोटी सी मदद चाहिए थी।",
]


def build_df(safe_list, scam_list, suspicious_list, augment_factor=4):
    rows = []
    for text in safe_list:
        rows.append({"text": text, "label": 0})
    for text in scam_list:
        rows.append({"text": text, "label": 1})
    for text in suspicious_list:
        rows.append({"text": text, "label": 2})

    # Simple augmentation: repeat with minor variations
    base = rows.copy()
    for _ in range(augment_factor - 1):
        for row in base:
            aug_text = row["text"]
            # Randomly strip/add trailing space or punctuation
            if random.random() < 0.3:
                aug_text = aug_text.strip() + " "
            elif random.random() < 0.3:
                aug_text = " " + aug_text.strip()
            rows.append({"text": aug_text, "label": row["label"]})

    df = pd.DataFrame(rows)
    df = df.sample(frac=1, random_state=42).reset_index(drop=True)
    return df


def main():
    os.makedirs("ml/data", exist_ok=True)

    # DistilBERT dataset (English/Hinglish)
    df_dist = build_df(ENGLISH_SAFE, ENGLISH_SCAM, ENGLISH_SUSPICIOUS)
    df_dist.to_csv("ml/data/distilbert_data.csv", index=False)
    print(f"[DistilBERT] Dataset: {len(df_dist)} samples  "
          f"(SAFE={len(df_dist[df_dist.label==0])}, "
          f"SCAM={len(df_dist[df_dist.label==1])}, "
          f"SUSPICIOUS={len(df_dist[df_dist.label==2])})")

    # IndicBERT dataset (Indic languages)
    df_indic = build_df(INDIC_SAFE, INDIC_SCAM, INDIC_SUSPICIOUS)
    df_indic.to_csv("ml/data/indicbert_data.csv", index=False)
    print(f"[IndicBERT]  Dataset: {len(df_indic)} samples  "
          f"(SAFE={len(df_indic[df_indic.label==0])}, "
          f"SCAM={len(df_indic[df_indic.label==1])}, "
          f"SUSPICIOUS={len(df_indic[df_indic.label==2])})")


if __name__ == "__main__":
    main()
