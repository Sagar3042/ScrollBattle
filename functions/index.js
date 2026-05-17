const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// ডাটাবেসের 'GlobalNotification' নোডের ওপর নজর রাখা
exports.sendAdminNotification = functions.database.ref('/GlobalNotification')
    .onWrite((change, context) => {
        const data = change.after.val();

        // যদি ডেটা না থাকে বা মেসেজ ফাঁকা হয়, তবে কিছু করবে না
        if (!data || !data.message) {
            return null;
        }

        // নোটিফিকেশনের প্যাকেজ তৈরি
        const payload = {
            notification: {
                title: "📢 Admin Announcement",
                body: data.message,
            },
            topic: "all_users" // যারা এই টপিকে সাবস্ক্রাইব করেছে (সবাই), তারা পাবে
        };

        // গুগলের FCM সার্ভারের মাধ্যমে ডাইরেক্ট নোটিফিকেশন সেন্ড করা
        return admin.messaging().send(payload)
            .then((response) => {
                console.log("Successfully sent notification:", response);
                return null;
            })
            .catch((error) => {
                console.error("Error sending notification:", error);
                return null;
            });
    });

