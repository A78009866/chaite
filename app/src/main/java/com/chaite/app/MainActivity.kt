package com.chaite.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

// --- إعدادات الألوان ---
val WhiteColor = Color(0xFFFFFFFF)
val LightGray = Color(0xFFF5F5F5)
val PrimaryBlue = Color(0xFF007AFF)
val DarkText = Color(0xFF1A1A1A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إعداد Cloudinary (يتم مرة واحدة)
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "duixjs8az" // اسم الكلاود الخاص بك
            config["api_key"] = "143978951428697" // API Key
            config["api_secret"] = "9dX6eIvntdtGQIU7oXGMSRG9I2o" // Secret (تحذير: وضعه هنا غير آمن في التطبيقات الحقيقية، يفضل استخدام التوقيع من السيرفر)
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // سبق التهيئة
        }

        setContent {
            ChaiteApp()
        }
    }
}

@Composable
fun ChaiteApp() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) "users" else "login"

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            background = WhiteColor,
            surface = WhiteColor
        )
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") { LoginScreen(navController) }
            composable("signup") { SignUpScreen(navController) }
            composable("users") { UsersListScreen(navController) }
            composable("chat/{userId}/{userName}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                ChatScreen(navController, userId, userName)
            }
        }
    }
}

// --- شاشة تسجيل الدخول ---
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Chaite", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        Spacer(Modifier.height(40.dp))
        
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة السر") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { navController.navigate("users") { popUpTo("login") { inclusive = true } } }
                    .addOnFailureListener { Toast.makeText(context, "خطأ: ${it.message}", Toast.LENGTH_SHORT).show() }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("دخول") }
        
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("signup") }) {
            Text("ليس لديك حساب؟ إنشاء حساب جديد")
        }
    }
}

// --- شاشة إنشاء حساب مع رفع صورة ---
@Composable
fun SignUpScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(LightGray).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
            if (imageUri != null) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("صورة", color = Color.Gray)
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("كلمة السر") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        if (isLoading) CircularProgressIndicator() else Button(
            onClick = {
                if (imageUri != null && email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener { res ->
                        val uid = res.user!!.uid
                        // رفع الصورة لـ Cloudinary
                        MediaManager.get().upload(imageUri).callback(object : UploadCallback {
                            override fun onStart(requestId: String) {}
                            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                val url = resultData["secure_url"].toString()
                                val userMap = hashMapOf("uid" to uid, "name" to name, "email" to email, "photoUrl" to url)
                                db.collection("users").document(uid).set(userMap).addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate("users") { popUpTo("signup") { inclusive = true } }
                                }
                            }
                            override fun onError(requestId: String, error: ErrorInfo) {
                                isLoading = false
                                Toast.makeText(context, "فشل رفع الصورة: ${error.description}", Toast.LENGTH_SHORT).show()
                            }
                            override fun onReschedule(requestId: String, error: ErrorInfo) {}
                        }).dispatch()
                    }.addOnFailureListener { 
                        isLoading = false
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() 
                    }
                } else {
                    Toast.makeText(context, "املأ جميع الحقول واختر صورة", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("إنشاء حساب") }
    }
}

// --- شاشة قائمة المستخدمين ---
@Composable
fun UsersListScreen(navController: NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("users").get().addOnSuccessListener { result ->
            users = result.documents.mapNotNull { it.data }.filter { it["uid"] != currentUserId }
        }
    }

    Column(Modifier.fillMaxSize().background(WhiteColor)) {
        Text("الدردشات", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(24.dp))
        LazyColumn {
            items(users) { user ->
                UserItem(user) {
                    navController.navigate("chat/${user["uid"]}/${user["name"]}")
                }
            }
        }
    }
}

@Composable
fun UserItem(user: Map<String, Any>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user["photoUrl"],
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape).background(LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Text(user["name"] as String, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

// --- شاشة المحادثة ---
@Composable
fun ChatScreen(navController: NavController, chatUserId: String, chatUserName: String) {
    val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
    val db = FirebaseFirestore.getInstance()
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    // إنشاء معرف فريد للمحادثة (ترتيب الآيديات أبجديًا)
    val chatId = if (currentUserId < chatUserId) "${currentUserId}_${chatUserId}" else "${chatUserId}_${currentUserId}"

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.documents.map { it.data!! }
                }
            }
    }

    Column(Modifier.fillMaxSize().background(WhiteColor)) {
        // Header
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(chatUserName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Divider(color = LightGray)

        // Messages List
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.Bottom) {
            items(messages) { msg ->
                val isMe = msg["senderId"] == currentUserId
                Box(Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(
                        text = msg["text"].toString(),
                        color = if (isMe) Color.White else DarkText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isMe) PrimaryBlue else LightGray)
                            .padding(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Input Area
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اكتب رسالة...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotEmpty()) {
                        val msg = hashMapOf(
                            "senderId" to currentUserId,
                            "text" to messageText,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                        db.collection("chats").document(chatId).collection("messages").add(msg)
                        messageText = ""
                    }
                },
                modifier = Modifier.background(PrimaryBlue, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}
