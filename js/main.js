console.log();
function changeFeatureImage(item, src) {
  document.getElementsByClassName("active")[0].classList.remove("active");
  item.classList.add("active");
  changeImage(src);
}

function changeImage(src) {
  document.getElementById("feature-img").src = "img/" + src;
}