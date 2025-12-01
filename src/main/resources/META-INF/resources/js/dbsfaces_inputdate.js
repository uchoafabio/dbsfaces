dbs_inputDate = function (pId) {
  var $root = $(pId);
  var $containers = $root.find("> .-container > .-th_input-data");

  if ($containers.length === 0) {
    return;
  }

  var updateFilledState = function ($container) {
    var hasValue = false;
    $container.find("input").each(function () {
      if (this.value && this.value.length > 0) {
        hasValue = true;
        return false;
      }
    });
    $container.toggleClass("-has-value", hasValue);
  };

  var parseHolidayList = function (rawValue) {
    if (!rawValue) {
      return [];
    }
    var parts = rawValue.split(/[,;]/);
    var seen = Object.create ? Object.create(null) : {};
    var dates = [];

    for (var i = 0; i < parts.length; i += 1) {
      var value = $.trim(parts[i]);
      if (!value || seen[value]) {
        continue;
      }
      seen[value] = true;
      dates.push(value);
    }
    return dates;
  };

  var attachHolidayLookup = function ($input) {
    var raw = $input.attr("data-holidays");
    var holidays = parseHolidayList(raw);
    if (!holidays.length) {
      return null;
    }
    var lookup = Object.create ? Object.create(null) : {};
    for (var i = 0; i < holidays.length; i += 1) {
      lookup[holidays[i]] = true;
    }
    $input.data("dbsHolidayLookup", lookup);
    return lookup;
  };

  var enforceHolidaySelection = function (input) {
    var $input = $(input);
    var lookup = $input.data("dbsHolidayLookup");
    if (!lookup) {
      if (typeof input.setCustomValidity === "function") {
        input.setCustomValidity("");
      }
      return;
    }
    var value = input.value;
    if (!value) {
      if (typeof input.setCustomValidity === "function") {
        input.setCustomValidity("");
      }
      return;
    }

    if (lookup[value]) {
      input.value = "";
      if (typeof input.setCustomValidity === "function") {
        input.setCustomValidity("Data indisponível");
        if (typeof input.reportValidity === "function") {
          input.reportValidity();
        }
        input.setCustomValidity("");
      }
      $input.trigger("dbs:inputdate:holiday", [value]);
    } else if (typeof input.setCustomValidity === "function") {
      input.setCustomValidity("");
    }
  };

  $containers.each(function () {
    updateFilledState($(this));
  });

  var $inputs = $containers.find("input");

  $inputs.each(function () {
    attachHolidayLookup($(this));
    enforceHolidaySelection(this);
  });

  $inputs
    .on("focus", function () {
      $(this).closest(".-th_input-data").addClass("-th_input-data-FOCUS");
    })
    .on("blur", function () {
      $(this).closest(".-th_input-data").removeClass("-th_input-data-FOCUS");
    })
    .on("input change", function () {
      enforceHolidaySelection(this);
      updateFilledState($(this).closest(".-th_input-data"));
    });
};
