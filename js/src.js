const versionNumber = "0.9.4";
document.getElementById("version").innerText = versionNumber;

function win_exe64(btn) {
  btn.disabled = true;
  incrementCounter({ "version": versionNumber, "platform": "win_exe64" })
}

function win_exe32(btn) {
  btn.disabled = true;
  incrementCounter({ "version": versionNumber, "platform": "win_exe32" })
}

function jar_portable(btn) {
  btn.disabled = true;
  incrementCounter({ "version": versionNumber, "platform": "jar" })
}

function incrementCounter(bodyData) {
  var requestOptions = {
    method: 'POST',
    headers: {
      "api_key": "AbdelRahmanBayomi",
      "Content-Type": "application/json"
    },
    body: JSON.stringify(bodyData)
  };
  fetch("https://azkar-download-tracker.herokuapp.com/downloads", requestOptions)
    .catch(error => console.log('error', error));
}

function getVersionCounter(version) {
  return fetch("https://azkar-download-tracker.herokuapp.com/downloads/" + version,
    {
      method: 'GET',
      headers: {
        "api_key": "AbdelRahmanBayomi",
        "Content-Type": "application/json"
      }
    }).then(result => result.json())
    .then(data => {
      // console.log("data:", data);
      if (data.version) {
        document.getElementById("win_exe64_counter").innerText = data.win_exe64;
        document.getElementById("win_exe32_counter").innerText = data.win_exe32;
        document.getElementById("jar_counter").innerText = data.jar;
        Array.from(document.getElementsByClassName("number-of-downloads")).forEach(
          (element, index, array) => {
            element.style.display = "inline";
          }
        );
      }
    }).catch(error => console.log('error', error));
}

let successAlert, dangerAlert;
document.addEventListener('DOMContentLoaded', () => {
  // fetch number of downloads for each platform
  getVersionCounter(versionNumber);

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


// -----------------------------------------------------------------------------
// ------------------------------ CONTACT US FORM ------------------------------
// -----------------------------------------------------------------------------

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
  dangerAlert.style.display = "none";
  successAlert.style.display = "block";
  successAlert.innerText = msg;
  document.querySelector('form').prepend(successAlert);;
}

function showFormError(msg) {
  successAlert.style.display = "none";
  dangerAlert.style.display = "block";
  dangerAlert.innerText = msg;
  document.querySelector('form').prepend(dangerAlert);;
}

function changeFeatureImage(item, src) {
  document.getElementsByClassName("active")[0].classList.remove("active");
  item.classList.add("active");
  document.getElementById("feature-img").src = "img/" + src;
}