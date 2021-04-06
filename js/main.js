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
  successAlert.remove();
  dangerAlert = document.getElementById("dangerAlert");
  dangerAlert.remove();

  document.querySelector('form').addEventListener('submit', event => {
    event.preventDefault();
    successAlert.remove();
    dangerAlert.remove();
    const name = document.getElementById("name").value;
    const email = document.getElementById("email").value;
    const message = document.getElementById("message").value;
    document.getElementById("name").value = "";
    document.getElementById("email").value = "";
    document.getElementById("message").value = "";
    submit(name, email, message);
  });
});


function checkEmail(elm) {
  var reg1 = /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,4}$/;
  if (reg1.test(elm.value) == false) {
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
  }).catch(() => {
    showFormError("Can't Send Your Response");
  });
}

function showFormSuccess(msg) {
  successAlert.innerText = msg;
  document.querySelector('form').prepend(successAlert);;
}
function showFormError(msg) {
  dangerAlert.innerText = msg;
  document.querySelector('form').prepend(dangerAlert);;
}