function changeFeatureImage(item, src) {
  document.getElementsByClassName("active")[0].classList.remove("active");
  item.classList.add("active");
  changeImage(src);
}

function changeImage(src) {
  document.getElementById("feature-img").src = "img/" + src;
}
let successAlert, dangerAlert;
document.addEventListener('DOMContentLoaded', () => {
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


function isMail(mailString) {
  var reg1 = /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,4}$/;
  return reg1.test(mailString);
}
function checkEmail(elm) {
  if (isMail(elm.value) == false) {
    elm.style.border = "1px solid red";
  } else {
    elm.style.border = "1px solid green";
  }
}

function submit(name, email, message) {
  // TODO validate data
  if (name == "" || email == "" || message == "") {
    showFormError("Please enter all Fields");
    return;
  }

  if(!isMail(email)){
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