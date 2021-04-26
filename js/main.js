// require('dotenv').config()

const versionNumber = "0.9.4";
document.getElementById("version").innerText = versionNumber;

function jar_portable() {
  console.log("jar_portable");
  incrementCounter(`"version": ${versionNumber}, "platform":"jar"`)
}

function win_exe64() {
  console.log("win_exe64");
  incrementCounter(`"version": ${versionNumber}, "platform":"win_exe64"`)
}

function win_exe32() {
  console.log("win_exe32");
  incrementCounter(`{"version": "${versionNumber}", "platform":"win_exe32"}`)
}

function incrementCounter(bodyData) {
  console.log("incrementCounter:", bodyData);

  // code for post request here
}


function changeFeatureImage(item, src) {
  document.getElementsByClassName("active")[0].classList.remove("active");
  item.classList.add("active");
  document.getElementById("feature-img").src = "img/" + src;
}

let successAlert, dangerAlert;
document.addEventListener('DOMContentLoaded', () => {
  // fetch number of downloads for each platform
  let win_exe64_counter = document.getElementById("win_exe64_counter");
  let win_exe32_counter = document.getElementById("win_exe32_counter");
  let jar_counter = document.getElementById("jar_counter");
  win_exe64_counter.innerText = 0;
  win_exe32_counter.innerText = 0;
  jar_counter.innerText = 0;

  // add action when form is submitted 
  successAlert = document.getElementById("successAlert");
  dangerAlert = document.getElementById("dangerAlert");
  document.querySelector('form').addEventListener('submit', event => {
    event.preventDefault();
    const name = document.getElementById("name").value;
    const email = document.getElementById("email").value;
    const message = document.getElementById("message").value;
    submit(name, email, message);
  });
});


// check if mail is correct
function isMail(mailString) {
  var reg1 = /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,4}$/;
  return reg1.test(mailString);
}

// submit contact-us form
function submit(name, email, message) {
  // TODO validate data
  if (name == "" || email == "" || message == "") {
    showFormError("Please enter all Fields");
    return;
  }

  if (!isMail(email)) {
    showFormError("Email is not correct");
    return;
  }
  name = encodeURIComponent(name);
  email = encodeURIComponent(email);
  message = encodeURIComponent(message);

  const formId = '1FAIpQLSeeXmLteMQ1EHwM77C3Eg_9ksFZb__yc3qzG6tCESAkLIcwEw';
  const queryString = '/formResponse?&entry.1748222099=' + name + '&entry.336933390=' + email
    + '&entry.1510931726=' + message;
  '&submit=SUBMIT'

  var url = 'https://docs.google.com/forms/d/e/' + formId + queryString;

  var opts = {
    method: "POST",
    mode: "no-cors", // apparently Google will only submit a form if "mode" is "no-cors"
    redirect: "follow",
    referrer: "no-referrer"
  }

  fetch(url, opts).then(() => {
    showFormSuccess("Sent Successfully")
    document.getElementById("name").value = "";
    document.getElementById("email").value = "";
    document.getElementById("message").value = "";
  }).catch(() => {
    showFormError("Can't Send Your Response");
  });
}

function showFormSuccess(msg) {
  console.log("showFormSuccess");
  dangerAlert.style.display = "none";
  successAlert.style.display = "block";
  successAlert.innerText = msg;
  document.querySelector('form').prepend(successAlert);;
}
function showFormError(msg) {
  console.log("showFormError");
  successAlert.style.display = "none";
  dangerAlert.style.display = "block";
  dangerAlert.innerText = msg;
  document.querySelector('form').prepend(dangerAlert);;
}
