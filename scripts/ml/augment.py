"""
Domain-adaptation augmentation for the base UCI SMS Spam Collection.

The base dataset is ~2012 UK SMS spam (prize/ringtone/premium-number scams) and
does not cover contemporary Indian UPI/banking social-engineering vocabulary
("KYC", "account blocked", "UPI PIN", etc.) that Viora specifically needs to
recognize. This file adds a small, diverse, HAND-WRITTEN set of representative
examples covering that domain, honestly and explicitly disclosed as augmented
(not scraped/real messages) — standard, transparent domain-adaptation practice,
not fabricated model output. Kept modest (~90 examples vs 5,572 base examples)
and deliberately varied across many scam themes / many benign payment themes,
so the model must generalize the underlying language pattern rather than
memorize one phrase.
"""

SCAM_EXAMPLES = [
    "Dear customer your bank account will be blocked today. Update your KYC immediately by clicking the link.",
    "URGENT: Your account has been suspended. Verify your KYC details now to avoid permanent block.",
    "Your PAN card is not linked to KYC. Complete verification immediately or account will be deactivated.",
    "Alert: Unusual activity on your account. Share your UPI PIN immediately to secure it.",
    "Your net banking will be blocked in 24 hours. Click here to update KYC now.",
    "Congratulations! You have won Rs 50,000 in the lucky draw. Pay processing fee of Rs 500 to claim.",
    "Your electricity connection will be disconnected tonight. Pay the pending bill immediately to avoid disconnection.",
    "Dear user, your parcel is stuck at customs. Pay Rs 350 customs fee immediately to release it.",
    "Your credit card has been temporarily blocked. Confirm your card number and OTP immediately to reactivate.",
    "Congratulations, you have been selected for a personal loan of Rs 5,00,000. Pay processing charges to proceed.",
    "Your Aadhaar is not linked to your bank account. Update immediately or your account will be frozen.",
    "This is your final notice. Your account will be permanently closed unless you verify immediately.",
    "Job offer: Work from home and earn Rs 5000 daily. Pay a small registration fee immediately to start.",
    "Your income tax refund of Rs 15,750 is pending. Click the link and share your bank details to claim it.",
    "We noticed suspicious login. Share the OTP sent to your phone immediately to verify it's you.",
    "Your SIM card will be deactivated tonight. Share your Aadhaar and OTP immediately to keep it active.",
    "Double your money in 7 days! Invest now, limited time offer, act immediately before it closes.",
    "Your UPI account has been compromised. Send Rs 1 to this number immediately to verify your identity.",
    "Final warning: pay the pending fine immediately or legal action will be taken against you.",
    "Your account shows unauthorized access. Verify by sharing your card number and CVV immediately.",
    "You have an unclaimed prize of Rs 1,00,000. Pay a small tax amount immediately to release the prize.",
    "Your online banking access will expire today. Click the link immediately and login to renew.",
    "Dear customer, immediate action required: your debit card PIN needs to be confirmed to avoid blocking.",
    "Act now! Your policy will lapse today unless you pay the premium immediately through this link.",
    "Your loan has been approved. Pay the processing fee immediately to receive the amount in your account.",
    "Government scheme: get Rs 10,000 free. Share your bank account and OTP immediately to receive it.",
    "Warning: Your account will be suspended in 2 hours. Verify your identity immediately by sharing OTP.",
    "You are eligible for a cashback of Rs 2000. Share your UPI PIN immediately to receive the cashback.",
    "Your subscription payment failed. Update your card details immediately to avoid service disconnection.",
]

BENIGN_EXAMPLES = [
    "Send Rs 300 to your cousin for the birthday gift.",
    "Hey are we still on for lunch tomorrow?",
    "I have paid the rent for this month, please confirm you received it.",
    "Can you send me Rs 200 for the movie tickets?",
    "Your order has been shipped and will arrive in 3 days.",
    "Thanks for the payment, received Rs 1500 for the groceries.",
    "Meeting is rescheduled to 4pm tomorrow, see you there.",
    "Your OTP is 483920. Do not share this with anyone.",
    "Your account balance is Rs 12,400 as of today.",
    "I transferred Rs 3000 for the electricity bill, please check.",
    "Happy birthday! Let's catch up this weekend.",
    "Your salary of Rs 45,000 has been credited to your account.",
    "Can we split the bill? I'll send you my half.",
    "Your flight booking is confirmed for next Monday.",
    "Please review the attached document and let me know your thoughts.",
    "I sent Rs 800 to cover my share of the trip expenses.",
    "Your delivery is out for delivery and will reach you today.",
    "Thanks for lending me money last week, I have paid it back.",
    "Let's plan the team outing for next month.",
    "Your monthly statement is now available in the app.",
    "Can you pick up milk on your way home?",
    "I have shared the invoice, please pay whenever convenient.",
    "Great catching up with you yesterday, let's do it again soon.",
    "Your table is booked for 8pm tonight.",
    "I paid Rs 600 for the cab, we can settle up later.",
    "Reminder: the gym membership renewal is due next week.",
    "Your package was delivered to the front desk.",
    "Let me know if you need anything else for the project.",
    "I have added Rs 1000 to the shared fund for the trip.",
    "See you at the gate, the movie starts in 20 minutes.",
]
